package com.garageos.modules.media.service;

import com.garageos.core.config.GoogleDriveProperties;
import com.garageos.modules.media.service.impl.GoogleDriveCredentialDataStore;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.DriveScopes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * OAuth Authorization Code flow (unchanged from the original implementation)
 * — client id/secret, redirect URI, "offline" access type, and the Drive
 * scope are all exactly as before. The only change is *where* the resulting
 * {@code StoredCredential} is persisted: previously a local-disk
 * FileDataStoreFactory directory (google-drive-tokens/, wiped on every
 * Render restart/redeploy); now {@link GoogleDriveCredentialDataStore},
 * backed by the application's own PostgreSQL database, which survives
 * restarts, redeploys, and is shared correctly across multiple instances.
 */
@Service
@Slf4j
public class GoogleDriveOAuthService {

    private static final String USER_ID =
            "garagest-drive";

    private final GoogleDriveProperties properties;
    private final GoogleDriveCredentialDataStore credentialDataStore;
    private final HttpTransport httpTransport;
    private final JsonFactory jsonFactory;

    // Root-cause fix (testability + consistency): this used to build its
    // own GoogleNetHttpTransport.newTrustedTransport() on every single
    // call instead of reusing the same HttpTransport/JsonFactory beans
    // GoogleDriveConfig already exposes and GoogleDriveClientService
    // already consumes. Besides the wasted per-call transport
    // construction, hardcoding the real network transport made this class
    // impossible to unit-test against credential-refresh behavior without
    // hitting Google's real servers — injecting it (like every other
    // Drive-facing service in this module already does) fixes both.
    public GoogleDriveOAuthService(
            GoogleDriveProperties properties,
            GoogleDriveCredentialDataStore credentialDataStore,
            HttpTransport httpTransport,
            JsonFactory jsonFactory) {

        this.properties = properties;
        this.credentialDataStore = credentialDataStore;
        this.httpTransport = httpTransport;
        this.jsonFactory = jsonFactory;
    }

    private GoogleAuthorizationCodeFlow createFlow()
            throws GeneralSecurityException, IOException {

        log.debug(
                "[DRIVE_AUTH] Creating Google authorization flow."
        );

        GoogleClientSecrets clientSecrets =
                new GoogleClientSecrets()
                        .setWeb(
                                new GoogleClientSecrets.Details()
                                        .setClientId(
                                                properties.getClientId()
                                        )
                                        .setClientSecret(
                                                properties.getClientSecret()
                                        )
                        );

        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        httpTransport,
                        jsonFactory,
                        clientSecrets,
                        Collections.singleton(
                                DriveScopes.DRIVE_FILE
                        )
                )
                        .setCredentialDataStore(
                                credentialDataStore
                        )
                        .setAccessType("offline")
                        .build();

        log.debug(
                "[DRIVE_AUTH] Google authorization flow created."
        );

        return flow;
    }

    public String getAuthorizationUrl()
            throws GeneralSecurityException, IOException {

        log.info(
                "[DRIVE_AUTH] Generating Google Drive authorization URL."
        );

        String authorizationUrl =
                createFlow()
                        .newAuthorizationUrl()
                        .setRedirectUri(
                                properties.getRedirectUri()
                        )
                        // Root-cause fix (permanent Drive auth, not
                        // per-run manual reauthorization): accessType
                        // "offline" alone is NOT sufficient to get a
                        // refresh_token back from Google on every
                        // authorization. Google's token endpoint only
                        // includes refresh_token in the very first
                        // consent for a given user+client pair — any
                        // later /authorize call (e.g. re-running this
                        // flow after the DB was reset, or after this
                        // exact bug, where a previous consent already
                        // happened) silently returns an access-token-only
                        // response, because Google recognizes the user
                        // already granted consent and skips re-issuing a
                        // refresh_token. Forcing prompt=consent makes
                        // Google show the consent screen and reissue a
                        // refresh_token EVERY time this URL is used,
                        // which is what actually makes the one-time-setup
                        // promise true going forward.
                        .set("prompt", "consent")
                        .build();

        log.info(
                "[DRIVE_AUTH] Google Drive authorization URL generated."
        );

        return authorizationUrl;
    }

    public void exchangeCode(String code)
            throws GeneralSecurityException, IOException {

        log.info(
                "[DRIVE_AUTH] Starting Google authorization code exchange."
        );

        GoogleAuthorizationCodeFlow flow =
                createFlow();

        GoogleTokenResponse tokenResponse =
                flow.newTokenRequest(code)
                        .setRedirectUri(
                                properties.getRedirectUri()
                        )
                        .execute();

        log.info(
                "[DRIVE_AUTH] Google authorization code exchanged successfully."
        );

        flow.createAndStoreCredential(
                tokenResponse,
                USER_ID
        );

        log.info(
                "[DRIVE_AUTH] Google Drive credential stored successfully."
        );
    }

    public Credential getStoredCredential()
            throws GeneralSecurityException, IOException {

        log.debug(
                "[DRIVE_AUTH] Loading stored credential. userId={}",
                USER_ID
        );

        GoogleAuthorizationCodeFlow flow =
                createFlow();

        Credential credential =
                flow.loadCredential(USER_ID);

        if (credential == null) {

            log.warn(
                    "[DRIVE_AUTH] No stored credential found. userId={}",
                    USER_ID
            );

        } else {

            log.debug(
                    "[DRIVE_AUTH] Stored credential loaded successfully. userId={}",
                    USER_ID
            );
        }

        return credential;
    }
}