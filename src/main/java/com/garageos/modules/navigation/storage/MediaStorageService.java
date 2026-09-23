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

    /**
     * Raw file bytes for a stored item - needed to actually serve trip
     * evidence over HTTP (getUrl() alone returns a path nothing was ever
     * mapped to serve; see NavigationTripMediaController's content
     * endpoint).
     */
    byte[] readBytes(String storageKey);
}