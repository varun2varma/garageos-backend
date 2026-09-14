package com.garageos.modules.media.service;

import com.garageos.core.config.GoogleDriveProperties;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
public class GoogleDriveFolderService {

    private static final String FOLDER_MIME_TYPE =
            "application/vnd.google-apps.folder";

    private final GoogleDriveClientService driveClientService;
    private final GoogleDriveProperties properties;

    public GoogleDriveFolderService(
            GoogleDriveClientService driveClientService,
            GoogleDriveProperties properties) {

        this.driveClientService = driveClientService;
        this.properties = properties;
    }

    public File getOrCreateFolder(
            String folderName,
            String parentFolderId)
            throws GeneralSecurityException, IOException {

        Drive drive = driveClientService.getDriveClient();

        String escapedFolderName =
                folderName.replace("'", "\\'");

        String query =
                "name = '" + escapedFolderName + "'"
                        + " and mimeType = '" + FOLDER_MIME_TYPE + "'"
                        + " and trashed = false"
                        + " and '" + parentFolderId + "' in parents";

        List<File> folders = drive.files()
                .list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id,name,parents,webViewLink)")
                .setPageSize(10)
                .execute()
                .getFiles();

        if (folders != null && !folders.isEmpty()) {
            return folders.get(0);
        }

        File folderMetadata = new File()
                .setName(folderName)
                .setMimeType(FOLDER_MIME_TYPE)
                .setParents(List.of(parentFolderId));

        return drive.files()
                .create(folderMetadata)
                .setFields("id,name,parents,webViewLink")
                .execute();
    }

    public File getOrCreateGarageFolder(
            String garageCode)
            throws GeneralSecurityException, IOException {

        return getOrCreateFolder(
                garageCode,
                properties.getRootFolderId()
        );
    }

    public File getOrCreateJobCardFolder(
            String garageCode,
            String jobCardNumber)
            throws GeneralSecurityException, IOException {

        File garageFolder =
                getOrCreateGarageFolder(garageCode);

        return getOrCreateFolder(
                jobCardNumber,
                garageFolder.getId()
        );
    }

    public File getOrCreateStageFolder(
            String garageCode,
            String jobCardNumber,
            String stage)
            throws GeneralSecurityException, IOException {

        File jobCardFolder =
                getOrCreateJobCardFolder(
                        garageCode,
                        jobCardNumber
                );

        return getOrCreateFolder(
                stage,
                jobCardFolder.getId()
        );
    }
}