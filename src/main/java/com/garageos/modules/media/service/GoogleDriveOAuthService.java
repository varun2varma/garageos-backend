package com.garageos.modules.media.service;

import com.garageos.core.config.GoogleDriveProperties;
import com.garageos.modules.media.service.impl.GoogleDriveCredentialDataStore;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
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

    public GoogleDriveOAuthService(
            GoogleDriveProperties properties,
            GoogleDriveCredentialDataStore credentialDataStore) {

        this.properties = properties;
        this.credentialDataStore = credentialDataStore;
    }

    private GoogleAuthorizationCodeFlow createFlow()
            throws GeneralSecurityException, IOException {

        log.debug(
                "[DRIVE_AUTH] Creating Google authorization flow."
        );

        var httpTransport =
                GoogleNetHttpTransport
                        .newTrustedTransport();

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
                        com.google.api.client.json.gson.GsonFactory
                                .getDefaultInstance(),
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