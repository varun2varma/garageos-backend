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

    /**
     * Stores already-validated bytes under {@code folder} with a
     * server-chosen {@code extension} (e.g. ".png"), so callers that have
     * sniffed the real file type never inherit a client-supplied filename.
     * Used for garage logos.
     */
    default String uploadBytes(byte[] content, String folder, String extension) {
        throw new UnsupportedOperationException(
                "uploadBytes is not supported by this storage service");
    }

    /** Best-effort delete of one stored object; a missing key is not an error. */
    default void delete(String storageKey) {
        // optional capability
    }
}