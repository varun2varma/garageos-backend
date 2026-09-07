package com.garageos.modules.navigation.storage;

import org.springframework.web.multipart.MultipartFile;

public interface MediaStorageService {

    String upload(
            MultipartFile file,
            String folder
    );

    String getUrl(
            String storageKey
    );
}