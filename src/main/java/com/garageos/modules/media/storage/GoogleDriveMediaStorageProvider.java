package com.garageos.modules.media.storage;

import com.garageos.core.enums.media.StorageProvider;
import com.garageos.modules.media.entity.JobCardMedia;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Thin adapter over the existing, already-working Google Drive integration
 * (GoogleDriveFileService/GoogleDriveClientService/GoogleDriveFolderService —
 * untouched by this class). Behavior for existing Drive-backed rows is
 * unchanged: {@code uploadMedia}/{@code downloadContent} in MediaServiceImpl
 * keep calling those services directly, exactly as before.
 *
 * This adapter exists only so the new, provider-neutral endpoints
 * (upload-intent/complete/access) can treat GOOGLE_DRIVE as one case of
 * {@link MediaStorageProvider} rather than a hardcoded assumption — in
 * particular so {@code GET /media/{id}/access} works uniformly for legacy
 * Drive rows too (returning the existing proxied content endpoint, since
 * Drive has no clean direct-to-client access model this codebase uses).
 *
 * Google Drive is never selected for NEW uploads (see
 * MediaServiceImpl.createUploadIntent) — this adapter's upload-authorization
 * method is intentionally unsupported.
 */
@Slf4j
@Component
public class GoogleDriveMediaStorageProvider implements MediaStorageProvider {

    @Override
    public StorageProvider getProviderType() {
        return StorageProvider.GOOGLE_DRIVE;
    }

    @Override
    public UploadAuthorization createUploadAuthorization(String storageKey, String contentType) {
        throw new UnsupportedOperationException(
                "Google Drive does not support direct client upload; new media uploads use R2."
        );
    }

    @Override
    public Long confirmUpload(String storageKey) {
        // Not used on this path — Drive uploads go through the existing
        // synchronous MediaServiceImpl.uploadMedia()/MediaUploadRetryService
        // flow, which never calls into this interface.
        throw new UnsupportedOperationException(
                "Google Drive uploads are not confirmed through this interface."
        );
    }

    @Override
    public PlaybackAccess createPlaybackAccess(JobCardMedia media) {
        return PlaybackAccess.builder()
                .url("/api/v1/job-cards/" + media.getJobCardId() + "/media/" + media.getId() + "/content")
                .direct(false)
                .headers(Map.of())
                .expiresAt(null)
                .available(true)
                .build();
    }

    @Override
    public void delete(JobCardMedia media) {
        // Known, already-documented limitation carried forward unchanged:
        // there is no Drive file-deletion capability in this codebase.
        // Deleting the JobCardMedia row (see MediaServiceImpl.deleteMedia)
        // does not remove the underlying Drive file.
        log.warn(
                "[MEDIA][DRIVE] Delete requested for a Google Drive-backed media row; "
                        + "the Drive file itself is NOT deleted (no delete capability exists). mediaId={}",
                media.getId()
        );
    }

    @Override
    public boolean exists(String storageKey) {
        throw new UnsupportedOperationException(
                "Existence checks are not implemented for Google Drive-backed media."
        );
    }

    @Override
    public byte[] downloadBytes(String storageKey) {
        // Not called on this path — MediaProcessingScheduler only processes
        // R2 rows (see its own doc comment: video/Drive thumbnail generation
        // is deliberately out of scope for this pass).
        throw new UnsupportedOperationException(
                "Thumbnail generation is not implemented for Google Drive-backed media."
        );
    }

    @Override
    public void uploadBytes(String storageKey, byte[] content, String contentType) {
        throw new UnsupportedOperationException(
                "Thumbnail generation is not implemented for Google Drive-backed media."
        );
    }
}
