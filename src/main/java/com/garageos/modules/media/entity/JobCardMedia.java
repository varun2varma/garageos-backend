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
}