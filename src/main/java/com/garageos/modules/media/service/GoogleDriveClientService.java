package com.garageos.modules.media.service;

import com.garageos.core.config.GoogleDriveProperties;
import com.garageos.core.exception.MediaException;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
@Slf4j
public class GoogleDriveClientService {

    private final HttpTransport httpTransport;
    private final JsonFactory jsonFactory;
    private final GoogleDriveProperties properties;
    private final GoogleDriveOAuthService oauthService;

    public GoogleDriveClientService(
            HttpTransport httpTransport,
            JsonFactory jsonFactory,
            GoogleDriveProperties properties,
            GoogleDriveOAuthService oauthService) {

        this.httpTransport = httpTransport;
        this.jsonFactory = jsonFactory;
        this.properties = properties;
        this.oauthService = oauthService;
    }

    public Drive getDriveClient()
            throws GeneralSecurityException, IOException {

        log.info("[DRIVE_AUTH] Loading stored Google Drive credential.");

        Credential credential =
                oauthService.getStoredCredential();

        if (credential == null) {

            log.error(
                    "[DRIVE_AUTH_CHECK] No stored Google Drive credential found. "
                            + "Drive has not been authorized, or the stored grant was removed."
            );

            throw new MediaException(
                    MediaException.MediaErrorCode.MEDIA_DRIVE_AUTH_REQUIRED,
                    "Google Drive has not been authorized for this server yet."
            );
        }

        log.info(
                "[DRIVE_AUTH] Stored Google Drive credential found."
        );

        log.debug(
                "[DRIVE_AUTH] Building Google Drive client. applicationName={}",
                properties.getApplicationName()
        );

        Drive drive =
                new Drive.Builder(
                        httpTransport,
                        jsonFactory,
                        credential
                )
                        .setApplicationName(
                                properties.getApplicationName()
                        )
                        .build();

        log.info(
                "[DRIVE_AUTH] Google Drive client created successfully."
        );

        return drive;
    }

    public String createGarageStRootFolder()
            throws GeneralSecurityException, IOException {

        log.info(
                "[DRIVE] Creating GarageST root folder."
        );

        Drive drive =
                getDriveClient();

        File folderMetadata = new File()
                .setName("GarageST")
                .setMimeType(
                        "application/vnd.google-apps.folder"
                );

        var folder =
                drive.files()
                        .create(folderMetadata)
                        .setFields(
                                "id, name, webViewLink"
                        )
                        .execute();

        log.info(
                "[DRIVE] GarageST root folder created. folderId={}",
                folder.getId()
        );

        return folder.getId();
    }

    /**
     * A Drive operation that returns {@code T}, checked-exception-throwing
     * so callers can use the same try/catch classification the rest of
     * this module already uses.
     */
    @FunctionalInterface
    public interface DriveCall<T> {
        T run(Drive drive) throws GeneralSecurityException, IOException;
    }

    /**
     * Root-cause fix for (A): a Drive call that fails with 401/403 (an
     * expired-but-refreshable access token, or a stale in-memory Credential
     * object) previously went straight to
     * MediaServiceImpl.toMediaException()'s AUTH_FAILED branch with no
     * attempt to refresh first. The Google API client's Credential class
     * already refreshes proactively inside intercept() when it *thinks* the
     * token is close to expiring, but that check is based on the client's
     * own clock/cached expiry — it does not cover a token Google has
     * already invalidated server-side for a reason the client can't see in
     * advance. Once, and only once, on a 401/403, this forces an explicit
     * {@code credential.refreshToken()} and retries the same Drive call
     * with the refreshed credential.
     *
     * If the refresh itself fails (refresh token invalid/revoked -
     * TokenResponseException), that failure is what propagates — it is not
     * masked, and there is deliberately no second retry: a bad refresh
     * token will not become good by retrying, so this must not loop. Any
     * other exception (timeout, 429, 5xx, generic IOException) is not
     * retried here at all — those are transient failures the caller queues
     * for a backoff retry (MediaUploadRetryService) rather than retrying
     * synchronously inside the request.
     */
    public <T> T executeWithAuthRetry(DriveCall<T> call)
            throws GeneralSecurityException, IOException {

        Credential credential = oauthService.getStoredCredential();

        if (credential == null) {

            log.error(
                    "[DRIVE_AUTH_CHECK] No stored Google Drive credential found. "
                            + "Drive has not been authorized, or the stored grant was removed."
            );

            throw new MediaException(
                    MediaException.MediaErrorCode.MEDIA_DRIVE_AUTH_REQUIRED,
                    "Google Drive has not been authorized for this server yet."
            );
        }

        Drive drive = buildDriveClient(credential);

        try {

            return call.run(drive);

        } catch (TokenResponseException authEx) {

            // The access token itself was rejected outright, not merely
            // expired-by-the-clock. A refresh attempt is still worth one
            // try (the stored refresh token may still be valid even though
            // the access token was rejected), but not more than one.
            return retryOnceAfterRefresh(credential, call, authEx);

        } catch (GoogleJsonResponseException googleEx) {

            int status = googleEx.getStatusCode();

            if (status == 401 || status == 403) {
                return retryOnceAfterRefresh(credential, call, googleEx);
            }

            throw googleEx;
        }
    }

    private <T> T retryOnceAfterRefresh(
            Credential credential,
            DriveCall<T> call,
            Exception originalFailure)
            throws GeneralSecurityException, IOException {

        log.warn(
                "[DRIVE_AUTH] Drive call rejected the current access token. "
                        + "Attempting one explicit refresh-and-retry."
        );

        boolean refreshed;

        try {

            refreshed = credential.refreshToken();

        } catch (TokenResponseException refreshFailure) {

            // The refresh token itself is invalid/revoked. Do not retry
            // again - this will not succeed on a second attempt, and
            // looping here would just hammer Google's token endpoint.
            log.error(
                    "[DRIVE_AUTH] Refresh token rejected by Google. Reauthorization is required."
            );

            throw refreshFailure;
        }

        if (!refreshed) {

            log.error(
                    "[DRIVE_AUTH] Credential refresh did not return a new token."
            );

            if (originalFailure instanceof IOException ioEx) {
                throw ioEx;
            }

            throw new IOException("Google Drive credential refresh failed.", originalFailure);
        }

        log.info("[DRIVE_AUTH] Access token refreshed. Retrying the Drive call once.");

        Drive refreshedDrive = buildDriveClient(credential);

        return call.run(refreshedDrive);
    }

    private Drive buildDriveClient(Credential credential) {

        return new Drive.Builder(
                httpTransport,
                jsonFactory,
                credential
        )
                .setApplicationName(
                        properties.getApplicationName()
                )
                .build();
    }
}