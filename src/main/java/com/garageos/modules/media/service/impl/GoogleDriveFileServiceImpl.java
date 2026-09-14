package com.garageos.modules.media.service.impl;

import com.garageos.modules.media.service.GoogleDriveFileService;
import com.garageos.modules.media.service.GoogleDriveClientService;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
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

        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("File is required.");
        }

        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name is required.");
        }

        if (parentFolderId == null || parentFolderId.isBlank()) {
            throw new IllegalArgumentException(
                    "Parent folder ID is required."
            );
        }

        Drive drive = driveClientService.getDriveClient();

        File fileMetadata = new File()
                .setName(fileName)
                .setParents(List.of(parentFolderId));

        String contentType = multipartFile.getContentType();

        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        try (InputStream inputStream =
                     multipartFile.getInputStream()) {

            InputStreamContent mediaContent =
                    new InputStreamContent(
                            contentType,
                            inputStream
                    );

            mediaContent.setLength(multipartFile.getSize());

            return drive.files()
                    .create(fileMetadata, mediaContent)
                    .setFields(
                            "id,name,mimeType,size,parents,webViewLink,createdTime"
                    )
                    .execute();
        }
    }
}