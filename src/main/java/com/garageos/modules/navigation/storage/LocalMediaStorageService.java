package com.garageos.modules.navigation.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalMediaStorageService
        implements MediaStorageService {

    @Value("${garageos.media.storage-path:uploads}")
    private String storagePath;

    @Override
    public String upload(
            MultipartFile file,
            String folder) {

        if (file == null
                || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Media file is required"
            );
        }

        try {

            Path folderPath =
                    Paths.get(
                            storagePath,
                            folder
                    );

            Files.createDirectories(
                    folderPath
            );

            String originalName =
                    file.getOriginalFilename();

            String extension =
                    extractExtension(
                            originalName
                    );

            String fileName =
                    UUID.randomUUID()
                            + extension;

            Path target =
                    folderPath.resolve(
                            fileName
                    );

            Files.copy(
                    file.getInputStream(),
                    target
            );

            return folder
                    + "/"
                    + fileName;

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Unable to store media file",
                    e
            );
        }
    }

    @Override
    public String getUrl(
            String storageKey) {

        return "/media/" + storageKey;
    }

    private String extractExtension(
            String fileName) {

        if (fileName == null
                || !fileName.contains(".")) {

            return "";
        }

        return fileName.substring(
                fileName.lastIndexOf(".")
        );
    }
}