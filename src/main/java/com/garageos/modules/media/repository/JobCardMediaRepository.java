package com.garageos.modules.media.repository;

import com.garageos.modules.media.entity.JobCardMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JobCardMediaRepository
        extends JpaRepository<JobCardMedia, Long> {

    List<JobCardMedia> findByJobCardIdOrderByCreatedAtAsc(
            Long jobCardId
    );

    List<JobCardMedia> findByJobCardIdAndMediaStageOrderByCreatedAtAsc(
            Long jobCardId,
            String mediaStage
    );

    List<JobCardMedia> findByRepairTaskIdOrderByCreatedAtAsc(
            Long repairTaskId
    );

    java.util.Optional<JobCardMedia> findByJobCardIdAndClientUploadId(Long jobCardId, String clientUploadId);

    /** Rows (other than {@code id}) that reference an object key as storage, evidence or thumbnail key. */
    @org.springframework.data.jpa.repository.Query(
            "select count(m) from JobCardMedia m where m.id <> :id "
                    + "and (m.storageKey = :key or m.evidenceKey = :key or m.thumbnailKey = :key "
                    + "or m.stagingSourceKey = :key)")
    long countOtherRowsReferencingKey(
            @org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("key") String key);

    /** Completed rows that still carry a staging source key awaiting deletion (restart-safe sweeper). */
    List<JobCardMedia> findByStorageProviderAndUploadStatusAndStagingSourceKeyIsNotNull(
            String storageProvider,
            String uploadStatus
    );

    List<JobCardMedia> findByJobCardIdAndVisibilityOrderByCreatedAtAsc(
            Long jobCardId,
            String visibility
    );

    /**
     * Retry scheduler's scan: rows durably queued for a Drive attempt whose
     * backoff window has elapsed. AUTH_REQUIRED is deliberately excluded —
     * those never auto-retry (see MediaUploadStatus) and only move back to
     * PENDING via {@link #wakeAuthRequiredRows}.
     */
    List<JobCardMedia> findByUploadStatusInAndNextRetryAtLessThanEqual(
            List<String> uploadStatuses,
            LocalDateTime now
    );

    /**
     * Called after a successful Google Drive reauthorization
     * (GoogleDriveOAuthService.exchangeCode) to give every row stuck in
     * AUTH_REQUIRED a fresh chance — without this, a row that hit
     * AUTH_REQUIRED stays there forever even after someone fixes the
     * underlying authorization, since AUTH_REQUIRED itself never
     * auto-retries.
     */
    @Modifying
    @Query("UPDATE JobCardMedia m SET m.uploadStatus = 'PENDING', "
            + "m.nextRetryAt = :now WHERE m.uploadStatus = 'AUTH_REQUIRED'")
    int wakeAuthRequiredRows(LocalDateTime now);

    /**
     * Idempotency lookup for the direct-upload completion endpoint — a
     * retried completion request with the same session id must return the
     * existing row rather than create a duplicate. Backed by the partial
     * unique index from V58.
     */
    Optional<JobCardMedia> findByUploadSessionId(String uploadSessionId);

    /** MediaProcessingScheduler's scan: R2 rows whose bytes are confirmed but not yet fully processed. */
    List<JobCardMedia> findByStorageProviderAndUploadStatus(
            String storageProvider,
            String uploadStatus
    );
}