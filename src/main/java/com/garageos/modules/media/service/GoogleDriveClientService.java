package com.garageos.modules.media.service;

import com.garageos.core.config.GoogleDriveProperties;
import com.google.api.client.auth.oauth2.Credential;
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
                    "[DRIVE_AUTH] No stored Google Drive credential found."
            );

            throw new IllegalStateException(
                    "Google Drive is not authorized. "
                            + "Please authorize Google Drive first."
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
}