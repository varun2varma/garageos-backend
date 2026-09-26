package com.garageos.modules.media.service;

import com.garageos.core.config.GoogleDriveProperties;
import com.google.api.services.drive.model.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
@Slf4j
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

        log.info(
                "[DRIVE] Resolving folder. folderName={}, parentFolderId={}",
                folderName,
                parentFolderId
        );

        String escapedFolderName =
                folderName.replace("'", "\\'");

        String query =
                "name = '" + escapedFolderName + "'"
                        + " and mimeType = '" + FOLDER_MIME_TYPE + "'"
                        + " and trashed = false"
                        + " and '" + parentFolderId + "' in parents";

        // Routed through the same executeWithAuthRetry wrapper the file
        // upload/download calls use (GoogleDriveFileServiceImpl), instead
        // of the raw getDriveClient() this used to call directly. Folder
        // resolution runs on every upload attempt before the file upload
        // itself, so an access token that merely expired between requests
        // used to skip the silent refresh-and-retry and go straight to
        // MediaUploadRetryServiceImpl's outer classifier, which cannot tell
        // "expired, refresh would have fixed it" apart from "genuinely
        // revoked" - unnecessarily landing on AUTH_REQUIRED instead of
        // transparently refreshing. Reuses the existing wrapper; no new
        // OAuth logic.
        return driveClientService.executeWithAuthRetry(drive -> {

            log.debug(
                    "[DRIVE] Searching for folder. folderName={}, parentFolderId={}",
                    folderName,
                    parentFolderId
            );

            List<File> folders = drive.files()
                    .list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id,name,parents,webViewLink)")
                    .setPageSize(10)
                    .execute()
                    .getFiles();

            if (folders != null && !folders.isEmpty()) {

                File existingFolder =
                        folders.get(0);

                log.info(
                        "[DRIVE] Existing folder found. folderName={}, folderId={}",
                        folderName,
                        existingFolder.getId()
                );

                return existingFolder;
            }

            log.info(
                    "[DRIVE] Folder not found. Creating folder. folderName={}, parentFolderId={}",
                    folderName,
                    parentFolderId
            );

            File folderMetadata = new File()
                    .setName(folderName)
                    .setMimeType(FOLDER_MIME_TYPE)
                    .setParents(List.of(parentFolderId));

            File createdFolder =
                    drive.files()
                            .create(folderMetadata)
                            .setFields("id,name,parents,webViewLink")
                            .execute();

            log.info(
                    "[DRIVE] Folder created successfully. folderName={}, folderId={}",
                    folderName,
                    createdFolder.getId()
            );

            return createdFolder;
        });
    }

    public File getOrCreateGarageFolder(
            String garageCode)
            throws GeneralSecurityException, IOException {

        log.info(
                "[DRIVE] Resolving garage folder. garageCode={}",
                garageCode
        );

        return getOrCreateFolder(
                garageCode,
                properties.getRootFolderId()
        );
    }

    public File getOrCreateJobCardFolder(
            String garageCode,
            String jobCardNumber)
            throws GeneralSecurityException, IOException {

        log.info(
                "[DRIVE] Resolving Job Card folder. garageCode={}, jobCardNumber={}",
                garageCode,
                jobCardNumber
        );

        File garageFolder =
                getOrCreateGarageFolder(garageCode);

        log.debug(
                "[DRIVE] Garage folder resolved. garageCode={}, folderId={}",
                garageCode,
                garageFolder.getId()
        );

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

        log.info(
                "[DRIVE] Resolving stage folder. garageCode={}, jobCardNumber={}, stage={}",
                garageCode,
                jobCardNumber,
                stage
        );

        File jobCardFolder =
                getOrCreateJobCardFolder(
                        garageCode,
                        jobCardNumber
                );

        log.debug(
                "[DRIVE] Job Card folder resolved. jobCardNumber={}, folderId={}",
                jobCardNumber,
                jobCardFolder.getId()
        );

        File stageFolder =
                getOrCreateFolder(
                        stage,
                        jobCardFolder.getId()
                );

        log.info(
                "[DRIVE] Stage folder resolved. stage={}, folderId={}",
                stage,
                stageFolder.getId()
        );

        return stageFolder;
    }
}