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

    /**
     * Stores already-processed evidence bytes (for example an orientation-normalised photo) and returns the storage
     * key. Cloud-capable implementations override this; the default keeps older implementations working.
     */
    /** Result of {@link #deleteObject}: the caller must be able to tell a real deletion from "was not there" and from a failure. */
    enum DeleteResult { DELETED, ALREADY_ABSENT, FAILED }

    /**
     * Deletes the object and reports what actually happened: {@code ALREADY_ABSENT} when it was not there,
     * {@code DELETED} only when it is confirmed gone afterwards, {@code FAILED} for any storage problem or when the
     * object is still there. Never throws for a storage problem. (The older {@link #delete} stays best-effort.)
     */
    default DeleteResult deleteObject(String storageKey) {
        throw new UnsupportedOperationException(
                "deleteObject is not supported by this storage service");
    }

    /** Whether the object exists; false only for a definitively missing object, other failures propagate. */
    default boolean exists(String storageKey) {
        throw new UnsupportedOperationException(
                "exists is not supported by this storage service");
    }

    /** Whether {@link #putObject} and {@link #exists} can be used (R2 configured); false for local-disk storage. */
    default boolean supportsObjectKeys() {
        return false;
    }

    /** Writes {@code content} at exactly {@code storageKey} (used to move evidence into its final folder). */
    default void putObject(String storageKey, byte[] content, String contentType) {
        throw new UnsupportedOperationException(
                "putObject is not supported by this storage service");
    }

    default String storeEvidence(byte[] content, String contentType, String folder, String extension) {
        return uploadBytes(content, folder, extension);
    }
}
