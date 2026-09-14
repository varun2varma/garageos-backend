package com.garageos.modules.media.service;

import com.garageos.core.config.GoogleDriveProperties;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleDriveOAuthService {

    private static final String TOKENS_DIRECTORY = "google-drive-tokens";
    private static final String USER_ID = "garagest-drive";

    private final GoogleDriveProperties properties;

    public GoogleDriveOAuthService(GoogleDriveProperties properties) {
        this.properties = properties;
    }

    private GoogleAuthorizationCodeFlow createFlow()
            throws GeneralSecurityException, IOException {

        var httpTransport =
                GoogleNetHttpTransport.newTrustedTransport();

        GoogleClientSecrets clientSecrets =
                new GoogleClientSecrets()
                        .setWeb(
                                new GoogleClientSecrets.Details()
                                        .setClientId(properties.getClientId())
                                        .setClientSecret(properties.getClientSecret())
                        );

        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
                clientSecrets,
                Collections.singleton(DriveScopes.DRIVE_FILE)
        )
                .setDataStoreFactory(
                        new FileDataStoreFactory(
                                new File(TOKENS_DIRECTORY)
                        )
                )
                .setAccessType("offline")
                .build();
    }

    public String getAuthorizationUrl()
            throws GeneralSecurityException, IOException {

        return createFlow()
                .newAuthorizationUrl()
                .setRedirectUri(properties.getRedirectUri())
                .build();
    }

    public void exchangeCode(String code)
            throws GeneralSecurityException, IOException {

        GoogleAuthorizationCodeFlow flow = createFlow();

        GoogleTokenResponse tokenResponse =
                flow.newTokenRequest(code)
                        .setRedirectUri(properties.getRedirectUri())
                        .execute();

        flow.createAndStoreCredential(
                tokenResponse,
                USER_ID
        );
    }

    public Credential getStoredCredential()
            throws GeneralSecurityException, IOException {

        GoogleAuthorizationCodeFlow flow = createFlow();

        return flow.loadCredential(USER_ID);
    }
}