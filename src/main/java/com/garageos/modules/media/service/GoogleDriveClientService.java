package com.garageos.modules.media.service;

import com.garageos.core.config.GoogleDriveProperties;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.Drive;
import org.springframework.stereotype.Service;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
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

        Credential credential =
                oauthService.getStoredCredential();

        if (credential == null) {
            throw new IllegalStateException(
                    "Google Drive is not authorized. "
                            + "Please authorize Google Drive first."
            );
        }

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

    public String createGarageStRootFolder()
            throws GeneralSecurityException, IOException {

        Drive drive = getDriveClient();

        File folderMetadata = new File()
                .setName("GarageST")
                .setMimeType("application/vnd.google-apps.folder");

        var folder = drive.files()
                .create(folderMetadata)
                .setFields("id, name, webViewLink")
                .execute();

        return folder.getId();
    }
}