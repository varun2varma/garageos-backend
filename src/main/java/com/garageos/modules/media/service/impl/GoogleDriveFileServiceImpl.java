package com.garageos.modules.media.service.impl;

import com.garageos.modules.media.service.GoogleDriveFileService;
import com.garageos.modules.media.service.GoogleDriveClientService;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
@Slf4j
public class GoogleDriveFileServiceImpl
        implements GoogleDriveFileService {

    private final GoogleDriveClientService driveClientService;

    public GoogleDriveFileServiceImpl(
            GoogleDriveClientService driveClientService) {

        this.driveClientService = driveClientService;
    }

    @Override
    public File uploadFile(
            MultipartFile multipartFile,
            String fileName,
            String parentFolderId)
            throws GeneralSecurityException, IOException {

        log.info(
                "[DRIVE] Preparing file upload. fileName={}, parentFolderId={}, size={}",
                fileName,
                parentFolderId,
                multipartFile != null
                        ? multipartFile.getSize()
                        : null
        );

        if (multipartFile == null || multipartFile.isEmpty()) {

            log.warn(
                    "[DRIVE] Upload file is missing or empty."
            );

            throw new IllegalArgumentException(
                    "File is required."
            );
        }

        if (fileName == null || fileName.isBlank()) {

            log.warn(
                    "[DRIVE] Upload file name is missing."
            );

            throw new IllegalArgumentException(
                    "File name is required."
            );
        }

        if (parentFolderId == null || parentFolderId.isBlank()) {

            log.warn(
                    "[DRIVE] Parent folder ID is missing. fileName={}",
                    fileName
            );

            throw new IllegalArgumentException(
                    "Parent folder ID is required."
            );
        }

        log.debug(
                "[DRIVE] Requesting Drive client. fileName={}",
                fileName
        );

        Drive drive =
                driveClientService.getDriveClient();

        log.info(
                "[DRIVE] Drive client obtained. Starting upload. fileName={}",
                fileName
        );

        File fileMetadata = new File()
                .setName(fileName)
                .setParents(List.of(parentFolderId));

        String contentType =
                multipartFile.getContentType();

        if (contentType == null
                || contentType.isBlank()) {

            contentType =
                    "application/octet-stream";
        }

        log.debug(
                "[DRIVE] File metadata prepared. fileName={}, contentType={}, parentFolderId={}",
                fileName,
                contentType,
                parentFolderId
        );

        try (InputStream inputStream =
                     multipartFile.getInputStream()) {

            InputStreamContent mediaContent =
                    new InputStreamContent(
                            contentType,
                            inputStream
                    );

            mediaContent.setLength(
                    multipartFile.getSize()
            );

            log.info(
                    "[DRIVE] Executing Drive file upload. fileName={}, contentType={}, size={}",
                    fileName,
                    contentType,
                    multipartFile.getSize()
            );

            File uploadedFile =
                    drive.files()
                            .create(
                                    fileMetadata,
                                    mediaContent
                            )
                            .setFields(
                                    "id,name,mimeType,size,parents,webViewLink,createdTime"
                            )
                            .execute();

            log.info(
                    "[DRIVE] Drive file upload successful. fileName={}, driveFileId={}",
                    fileName,
                    uploadedFile.getId()
            );

            return uploadedFile;

        } catch (IOException ex) {

            log.error(
                    "[DRIVE] Drive file upload failed. fileName={}, parentFolderId={}, error={}",
                    fileName,
                    parentFolderId,
                    ex.getMessage(),
                    ex
            );

            throw ex;
        }
    }
}