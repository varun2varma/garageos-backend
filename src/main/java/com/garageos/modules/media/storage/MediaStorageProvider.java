package com.garageos.modules.media.storage;

import com.garageos.core.enums.media.StorageProvider;
import com.garageos.modules.media.entity.JobCardMedia;

/**
 * Provider-neutral seam between the media domain/service layer (which knows
 * WHAT is being accessed — authorization, garage/job-card ownership, media
 * identity) and a concrete storage backend (which knows HOW bytes are
 * actually stored/served).
 *
 * Not to be confused with {@link com.garageos.modules.navigation.storage.MediaStorageService},
 * which is a local-disk buffer used for (a) navigation trip media generally
 * and (b) job-card media's own pending-upload staging area before a Drive
 * attempt — a different, lower-level concern than "which final storage
 * provider owns this media's bytes long-term".
 *
 * {@link MediaServiceImpl} decides WHICH implementation to use per media
 * item (via {@code storageProvider}); it never duplicates upload/download/
 * authorization logic per provider — that logic lives here, once per
 * provider.
 */
public interface MediaStorageProvider {

    StorageProvider getProviderType();

    /**
     * Issues a short-lived authorization for a client to upload directly to
     * this provider. Only meaningful for providers that support direct
     * client upload; throws {@link UnsupportedOperationException} otherwise
     * (see {@link GoogleDriveMediaStorageProvider}).
     */
    UploadAuthorization createUploadAuthorization(
            String storageKey,
            String contentType
    );

    /**
     * Confirms the object named by {@code storageKey} actually exists at
     * this provider (defense against a client reporting "done" without
     * having actually uploaded anything). Returns the object's actual size
     * as observed by the provider, or null if the provider can't report one
     * without a full download (Drive's case).
     */
    Long confirmUpload(String storageKey);

    PlaybackAccess createPlaybackAccess(JobCardMedia media);

    /**
     * Server-side read of an object's full bytes — used only by
     * {@link com.garageos.modules.media.service.impl.MediaProcessingScheduler}
     * to generate an image thumbnail. NOT used on any client-facing request
     * path (playback always goes through {@link #createPlaybackAccess},
     * never proxied bytes). Providers that can't support this in a
     * cheap/safe way (Drive — never called on this path today) may throw
     * {@link UnsupportedOperationException}.
     */
    byte[] downloadBytes(String storageKey);

    /**
     * Server-side write of a small derived asset (a generated thumbnail) —
     * NOT the client upload path (that's {@link #createUploadAuthorization},
     * a direct client-to-storage PUT). Only ever called with a small
     * (thumbnail-sized) byte array from
     * {@link com.garageos.modules.media.service.impl.MediaProcessingScheduler}.
     */
    void uploadBytes(String storageKey, byte[] content, String contentType);

    /** Best-effort deletion. Providers with no delete capability (Drive, currently) may no-op. */
    void delete(JobCardMedia media);

    boolean exists(String storageKey);

    /**
     * Whether this provider can actually be used right now. R2 overrides
     * this to check its environment configuration; Drive (never chosen for
     * new uploads) leaves the default.
     */
    default boolean isAvailable() {
        return true;
    }
}
