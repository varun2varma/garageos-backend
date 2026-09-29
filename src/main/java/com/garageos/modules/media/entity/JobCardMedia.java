package com.garageos.modules.media.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "job_card_media",
        indexes = {
                @Index(
                        name = "idx_job_card_media_job_card",
                        columnList = "job_card_id"
                ),
                @Index(
                        name = "idx_job_card_media_stage",
                        columnList = "job_card_id, media_stage"
                ),
                @Index(
                        name = "idx_job_card_media_type",
                        columnList = "job_card_id, media_type"
                ),
                @Index(
                        name = "idx_job_card_media_drive_file",
                        columnList = "drive_file_id"
                ),
                @Index(
                        name = "idx_job_card_media_repair_task",
                        columnList = "repair_task_id"
                ),
                @Index(
                        name = "idx_job_card_media_visibility",
                        columnList = "job_card_id, visibility"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobCardMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_card_id", nullable = false)
    private Long jobCardId;

    @Column(name = "repair_task_id")
    private Long repairTaskId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * Nullable as of V56: a row is now inserted before the Drive attempt
     * runs (see MediaServiceImpl.uploadMedia), so this is unset while
     * {@code uploadStatus} is PENDING/UPLOADING/RETRY_WAIT/AUTH_REQUIRED and
     * only becomes authoritative once {@code uploadStatus} is COMPLETED.
     */
    @Column(name = "drive_file_id", length = 255)
    private String driveFileId;

    @Column(name = "drive_web_view_link")
    private String driveWebViewLink;

    /**
     * Durability state of the Drive upload for this row. See
     * {@link com.garageos.core.enums.media.MediaUploadStatus}. Stored as a
     * plain string, matching every other enum-backed column on this entity
     * (mediaType, mediaStage, visibility).
     */
    @Column(name = "upload_status", nullable = false, length = 20)
    @Builder.Default
    private String uploadStatus = "COMPLETED";

    /** Number of failed Drive attempts so far (drives the backoff schedule). */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /** When the next backoff retry is eligible to run; null once COMPLETED/FAILED/AUTH_REQUIRED. */
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    /**
     * Sanitized failure summary from the most recent Drive attempt (never a
     * token value or raw Google exception message — see
     * MediaServiceImpl.toMediaException/classifyDriveFailure).
     */
    @Column(name = "last_error", length = 500)
    private String lastError;

    /**
     * Path/key into {@link com.garageos.modules.navigation.storage.MediaStorageService}
     * where the original uploaded bytes are durably buffered until the
     * Drive upload completes — so a failed/retried attempt re-uploads the
     * same bytes the caller actually sent, not a re-request from the
     * client. Cleared once uploadStatus is COMPLETED.
     */
    @Column(name = "local_storage_path", length = 500)
    private String localStoragePath;

    @Column(name = "media_type", nullable = false, length = 20)
    private String mediaType;

    @Column(name = "media_stage", nullable = false, length = 30)
    private String mediaStage;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "visibility", nullable = false, length = 30)
    @Builder.Default
    private String visibility = "INTERNAL";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Which storage backend actually holds this media's bytes — see
     * {@link com.garageos.core.enums.media.StorageProvider}. Stored as a
     * plain string, matching every other enum-backed column on this entity.
     * Defaults GOOGLE_DRIVE (V58 migration) so every row that existed before
     * this column was added keeps working through the existing Drive path
     * unchanged.
     */
    @Column(name = "storage_provider", nullable = false, length = 20)
    @Builder.Default
    private String storageProvider = "GOOGLE_DRIVE";

    /**
     * Provider-neutral object key (R2's equivalent of {@code driveFileId}).
     * Null for GOOGLE_DRIVE rows, where {@code driveFileId} remains
     * authoritative instead.
     */
    @Column(name = "storage_key", length = 500)
    private String storageKey;

    /** Client-reported checksum for the uploaded object, where available. */
    @Column(name = "checksum", length = 128)
    private String checksum;

    /**
     * Correlates a direct-upload's intent/complete request pair, and is the
     * idempotency key {@code completeUpload} dedups on — a retried
     * completion call with the same session id returns the existing row
     * rather than creating a duplicate. Null for GOOGLE_DRIVE rows (the
     * legacy multipart endpoint has no separate intent/complete steps).
     */
    @Column(name = "upload_session_id", length = 100)
    private String uploadSessionId;

    /** Object key of a generated thumbnail, once async processing has produced one. Null until then. */
    @Column(name = "thumbnail_key", length = 500)
    private String thumbnailKey;

    /** Video duration, once known. Null for images and for not-yet-processed videos. */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    // -----------------------------------------------------------------
    // AUDIT / EVIDENCE METADATA (V59) — WHO/WHEN/WHERE this media was
    // actually captured, distinct from WHEN/WHO it was uploaded (a
    // technician may capture media offline and the queue uploads it much
    // later — see MediaUploadQueueService). NULL on every row that
    // predates this column set (all legacy Drive rows, and any R2 row from
    // before this feature) — never fabricated retroactively.
    //
    // Immutability: there is deliberately no endpoint that updates any of
    // these fields after creation — they are set once, at
    // MediaServiceImpl.createUploadIntent/completeUpload, and never
    // touched again by any other code path. This is the actual enforcement
    // mechanism (no route to reach), not a runtime check.
    // -----------------------------------------------------------------

    /**
     * When the media was actually captured/selected on the device — NOT
     * when the upload completed. Client-reported (the device's own clock
     * at the moment of picking); trusted for audit context the same way a
     * camera's own EXIF timestamp would be, not treated as a
     * security-sensitive identity claim.
     */
    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    /**
     * Backend-authoritative — set from the authenticated principal at
     * upload-intent creation time, never accepted from the client request
     * body. See MediaServiceImpl.createUploadIntent.
     */
    @Column(name = "captured_by_user_id")
    private Long capturedByUserId;

    /** Name snapshot at capture time, so the display name survives the user later being renamed/deactivated. */
    @Column(name = "captured_by_name_snapshot", length = 200)
    private String capturedByNameSnapshot;

    /** Client-reported GPS latitude at capture time. Null if location was unavailable/denied — never fabricated. */
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "location_accuracy_meters")
    private Double locationAccuracyMeters;

    /** Human-readable reverse-geocoded snapshot (e.g. "Gachibowli, Hyderabad"). Coordinates remain authoritative. */
    @Column(name = "location_name", length = 255)
    private String locationName;

    /** When the upload actually completed (server clock) — set in MediaServiceImpl.completeUpload. */
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    /** Name snapshot of {@code uploadedBy} at completion time, same reasoning as {@code capturedByNameSnapshot}. */
    @Column(name = "uploaded_by_name_snapshot", length = 200)
    private String uploadedByNameSnapshot;

    /**
     * Object key of a generated "evidence" image — the original with a
     * tasteful metadata footer overlay (location/time/captured-by/stage),
     * NEVER a modification of {@code original}. Null until generated (or
     * for video, where this isn't generated — see MediaProcessingScheduler).
     */
    @Column(name = "evidence_key", length = 500)
    private String evidenceKey;
}