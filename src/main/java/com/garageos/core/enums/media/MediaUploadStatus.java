package com.garageos.core.enums.media;

/**
 * Durability state of a {@code JobCardMedia} row's underlying Google Drive
 * upload, independent of whether the original HTTP request that created the
 * row is still open.
 *
 * Added to fix two related defects: (1) a Drive-auth failure
 * (MEDIA_DRIVE_AUTH_FAILED) had no recovery path other than the caller
 * re-sending the whole file after someone reauthorized Drive, and (2) a
 * transient Drive failure (network blip, 429, 5xx) or a dropped client
 * connection mid-upload left nothing durable behind — the file bytes and any
 * record of the attempt were simply gone. See MediaServiceImpl.uploadMedia
 * and MediaUploadRetryServiceImpl.
 */
public enum MediaUploadStatus {

    /** Bytes are durably buffered locally; no Drive attempt has run yet. */
    PENDING,

    /** A Drive attempt is currently in flight for this row. */
    UPLOADING,

    /**
     * R2 path only: the client has confirmed a direct-to-storage upload and
     * the backend has verified the object exists at {@code storageKey}, but
     * derived assets (thumbnail, duration) have not been generated yet. The
     * original is already safely stored and could be served as-is; this
     * state exists so the UI can distinguish "bytes are safe" from "fully
     * ready", not because the media is unusable. See
     * MediaProcessingScheduler.
     */
    UPLOADED,

    /** R2 path only: a processing attempt (thumbnail/derived-asset generation) is in flight. */
    PROCESSING,

    /** Drive accepted the file (driveFileId set); or R2 processing finished. Fully ready either way. */
    COMPLETED,

    /** A Drive attempt failed with a transient error; a backoff retry is scheduled. */
    RETRY_WAIT,

    /**
     * Drive rejected the stored authorization (expired/revoked refresh
     * token). Does not auto-retry — requires reauthorizing Drive via
     * GoogleDriveOAuthController, which then wakes rows in this state back
     * to PENDING.
     */
    AUTH_REQUIRED,

    /** Backoff retries were exhausted without success. Requires manual intervention. */
    FAILED
}
