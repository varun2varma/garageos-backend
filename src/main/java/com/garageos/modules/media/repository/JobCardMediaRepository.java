package com.garageos.modules.media.repository;

import com.garageos.modules.media.entity.JobCardMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

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
}