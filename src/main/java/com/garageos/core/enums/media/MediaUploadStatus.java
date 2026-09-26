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

    /** Drive accepted the file; {@code driveFileId} is set and authoritative. */
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
