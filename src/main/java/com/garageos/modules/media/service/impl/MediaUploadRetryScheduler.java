package com.garageos.modules.media.service.impl;

import com.garageos.modules.media.entity.JobCardMedia;
import com.garageos.modules.media.repository.JobCardMediaRepository;
import com.garageos.modules.media.service.MediaUploadRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Executes the backoff retry queue for job-card media that failed to reach
 * Google Drive on its first (synchronous, in-request) attempt. This is the
 * only {@code @Scheduled} job in the application — there was no existing
 * scheduler/job-runner infrastructure to reuse (no other {@code @Scheduled}
 * method, no Quartz, in this codebase), so this adds the smallest possible
 * one using Spring's own built-in scheduling support.
 *
 * Fixed-delay poll rather than one timer per row: with the expected upload
 * volume for a single garage's job cards, a full-table scan for a handful
 * of eligible rows every 15s is negligible, and it avoids introducing any
 * new per-row scheduling primitive.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MediaUploadRetryScheduler {

    private static final List<String> RETRYABLE_STATUSES =
            List.of("PENDING", "RETRY_WAIT");

    private final JobCardMediaRepository jobCardMediaRepository;
    private final MediaUploadRetryService mediaUploadRetryService;

    @Scheduled(fixedDelay = 15_000L)
    public void retryEligibleUploads() {

        List<JobCardMedia> eligible =
                jobCardMediaRepository
                        .findByUploadStatusInAndNextRetryAtLessThanEqual(
                                RETRYABLE_STATUSES,
                                LocalDateTime.now()
                        );

        if (eligible.isEmpty()) {
            return;
        }

        log.info(
                "[MEDIA_RETRY_SCHEDULER] {} media row(s) eligible for a Drive retry.",
                eligible.size()
        );

        for (JobCardMedia media : eligible) {

            try {

                mediaUploadRetryService.attemptUpload(media);

            } catch (Exception ex) {

                // MediaUploadRetryService.attemptUpload already classifies
                // and persists every Drive/auth failure itself; reaching
                // here means something unrelated to Drive went wrong (e.g.
                // a DB error saving the row). Log and move on to the next
                // row rather than letting one bad row abort the whole scan.
                log.error(
                        "[MEDIA_RETRY_SCHEDULER] Unexpected failure retrying mediaId={}. error={}",
                        media.getId(),
                        ex.getMessage(),
                        ex
                );
            }
        }
    }
}
