package com.garageos.modules.media.service.impl;

import com.garageos.modules.media.service.GoogleDriveFileService;
import com.garageos.modules.media.service.GoogleDriveClientService;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
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

        String finalContentType = contentType;

        log.info(
                "[DRIVE] Executing Drive file upload. fileName={}, contentType={}, size={}",
                fileName,
                contentType,
                multipartFile.getSize()
        );

        File uploadedFile =
                driveClientService.executeWithAuthRetry(drive -> {

                    File fileMetadata = new File()
                            .setName(fileName)
                            .setParents(List.of(parentFolderId));

                    try (InputStream inputStream =
                                 multipartFile.getInputStream()) {

                        InputStreamContent mediaContent =
                                new InputStreamContent(
                                        finalContentType,
                                        inputStream
                                );

                        mediaContent.setLength(
                                multipartFile.getSize()
                        );

                        return drive.files()
                                .create(
                                        fileMetadata,
                                        mediaContent
                                )
                                .setFields(
                                        "id,name,mimeType,size,parents,webViewLink,createdTime"
                                )
                                .execute();
                    }
                });

        log.info(
                "[DRIVE] Drive file upload successful. fileName={}, driveFileId={}",
                fileName,
                uploadedFile.getId()
        );

        return uploadedFile;
    }

    @Override
    public File uploadBytes(
            byte[] content,
            String contentType,
            String fileName,
            String parentFolderId)
            throws GeneralSecurityException, IOException {

        log.info(
                "[DRIVE] Preparing buffered-bytes upload (retry path). fileName={}, parentFolderId={}, size={}",
                fileName,
                parentFolderId,
                content != null ? content.length : null
        );

        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("File content is required.");
        }

        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name is required.");
        }

        if (parentFolderId == null || parentFolderId.isBlank()) {
            throw new IllegalArgumentException("Parent folder ID is required.");
        }

        String effectiveContentType =
                (contentType == null || contentType.isBlank())
                        ? "application/octet-stream"
                        : contentType;

        File uploadedFile =
                driveClientService.executeWithAuthRetry(drive -> {

                    File fileMetadata = new File()
                            .setName(fileName)
                            .setParents(List.of(parentFolderId));

                    ByteArrayContent mediaContent =
                            new ByteArrayContent(effectiveContentType, content);

                    return drive.files()
                            .create(fileMetadata, mediaContent)
                            .setFields(
                                    "id,name,mimeType,size,parents,webViewLink,createdTime"
                            )
                            .execute();
                });

        log.info(
                "[DRIVE] Buffered-bytes upload successful. fileName={}, driveFileId={}",
                fileName,
                uploadedFile.getId()
        );

        return uploadedFile;
    }

    @Override
    public byte[] downloadFile(
            String driveFileId)
            throws GeneralSecurityException, IOException {

        log.info(
                "[DRIVE] Preparing file download. driveFileId={}",
                driveFileId
        );

        if (driveFileId == null || driveFileId.isBlank()) {

            log.warn(
                    "[DRIVE] Download requested with a missing Drive file id."
            );

            throw new IllegalArgumentException(
                    "Drive file id is required."
            );
        }

        log.info(
                "[DRIVE] Starting download with auth-retry. driveFileId={}",
                driveFileId
        );

        byte[] content =
                driveClientService.executeWithAuthRetry(drive -> {

                    try (InputStream mediaStream =
                                 drive.files()
                                         .get(driveFileId)
                                         .executeMediaAsInputStream();
                         ByteArrayOutputStream buffer =
                                 new ByteArrayOutputStream()) {

                        mediaStream.transferTo(buffer);

                        return buffer.toByteArray();
                    }
                });

        log.info(
                "[DRIVE] Drive file download successful. driveFileId={}, size={}",
                driveFileId,
                content.length
        );

        return content;
    }
}