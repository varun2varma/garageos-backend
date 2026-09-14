package com.garageos.modules.media.service;

import com.garageos.core.config.GoogleDriveProperties;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
@Slf4j
public class GoogleDriveOAuthService {

    private static final String TOKENS_DIRECTORY =
            "google-drive-tokens";

    private static final String USER_ID =
            "garagest-drive";

    private final GoogleDriveProperties properties;

    public GoogleDriveOAuthService(
            GoogleDriveProperties properties) {

        this.properties = properties;
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
                        .setDataStoreFactory(
                                new FileDataStoreFactory(
                                        new File(TOKENS_DIRECTORY)
                                )
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