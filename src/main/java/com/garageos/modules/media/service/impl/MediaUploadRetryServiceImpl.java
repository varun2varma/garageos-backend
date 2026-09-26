package com.garageos.modules.media.service.impl;

import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.media.entity.JobCardMedia;
import com.garageos.modules.media.repository.JobCardMediaRepository;
import com.garageos.modules.media.service.DriveFailureClassifier;
import com.garageos.modules.media.service.GoogleDriveFileService;
import com.garageos.modules.media.service.GoogleDriveFolderService;
import com.garageos.modules.media.service.MediaUploadRetryService;
import com.garageos.modules.navigation.storage.MediaStorageService;
import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Backoff schedule (task spec): 30s, 2m, 5m, 15m, 30m for transient Drive
 * failures. AUTH failures never auto-retry (require reauthorization via
 * GoogleDriveOAuthController, which then calls {@link #wakeAuthRequiredRows}).
 * Exhausting the schedule (a 6th consecutive transient failure) is a
 * terminal FAILED — left for manual investigation rather than retried
 * forever.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaUploadRetryServiceImpl implements MediaUploadRetryService {

    private static final long[] BACKOFF_SECONDS = {30L, 120L, 300L, 900L, 1800L};

    private static final String PENDING_UPLOAD_FOLDER = "job-card-media/pending";

    private final JobCardMediaRepository jobCardMediaRepository;
    private final JobCardRepository jobCardRepository;
    private final GoogleDriveFolderService folderService;
    private final GoogleDriveFileService googleDriveFileService;
    private final MediaStorageService mediaStorageService;

    @Override
    @Transactional
    public JobCardMedia attemptUpload(JobCardMedia media) {

        log.info(
                "[MEDIA_RETRY] Attempting Drive upload. mediaId={}, jobCardId={}, retryCount={}, uploadStatus={}",
                media.getId(),
                media.getJobCardId(),
                media.getRetryCount(),
                media.getUploadStatus()
        );

        media.setUploadStatus("UPLOADING");
        jobCardMediaRepository.save(media);

        try {

            if (media.getLocalStoragePath() == null) {

                // Should not happen (uploadMedia always buffers before the
                // row exists) - if it does, there is nothing to upload and
                // no point retrying, so fail it terminally rather than
                // spinning forever.
                throw new IllegalStateException(
                        "No buffered file for media id " + media.getId()
                );
            }

            JobCard jobCard =
                    jobCardRepository.findById(media.getJobCardId())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Job Card not found for media id " + media.getId()));

            String garageCode = jobCard.getGarage().getGarageCode();
            String jobCardNumber = jobCard.getJobCardNumber();

            File driveFolder =
                    folderService.getOrCreateStageFolder(
                            garageCode,
                            jobCardNumber,
                            media.getMediaStage()
                    );

            byte[] content =
                    mediaStorageService.readBytes(media.getLocalStoragePath());

            File driveFile =
                    googleDriveFileService.uploadBytes(
                            content,
                            media.getContentType(),
                            media.getFileName(),
                            driveFolder.getId()
                    );

            media.setDriveFileId(driveFile.getId());
            media.setDriveWebViewLink(driveFile.getWebViewLink());
            media.setUploadStatus("COMPLETED");
            media.setNextRetryAt(null);
            media.setLastError(null);

            JobCardMedia saved = jobCardMediaRepository.save(media);

            log.info(
                    "[MEDIA_RETRY] Drive upload succeeded. mediaId={}, driveFileId={}",
                    media.getId(),
                    driveFile.getId()
            );

            return saved;

        } catch (Exception ex) {

            return handleFailure(media, ex);
        }
    }

    private JobCardMedia handleFailure(JobCardMedia media, Exception ex) {

        DriveFailureClassifier.Classification classification =
                DriveFailureClassifier.classify(ex);

        String sanitizedMessage = DriveFailureClassifier.sanitizedMessage(ex);

        log.error(
                "[MEDIA_RETRY] Drive upload attempt failed. mediaId={}, classification={}, error={}",
                media.getId(),
                classification,
                ex.getMessage(),
                ex
        );

        media.setLastError(sanitizedMessage);

        if (classification == DriveFailureClassifier.Classification.AUTH) {

            media.setUploadStatus("AUTH_REQUIRED");
            media.setNextRetryAt(null);

            return jobCardMediaRepository.save(media);
        }

        int nextRetryCount = media.getRetryCount() == null ? 1 : media.getRetryCount() + 1;
        media.setRetryCount(nextRetryCount);

        if (nextRetryCount > BACKOFF_SECONDS.length) {

            log.error(
                    "[MEDIA_RETRY] Backoff exhausted, marking FAILED. mediaId={}, retryCount={}",
                    media.getId(),
                    nextRetryCount
            );

            media.setUploadStatus("FAILED");
            media.setNextRetryAt(null);

            return jobCardMediaRepository.save(media);
        }

        long delaySeconds = BACKOFF_SECONDS[nextRetryCount - 1];

        media.setUploadStatus("RETRY_WAIT");
        media.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySeconds));

        log.warn(
                "[MEDIA_RETRY] Scheduling backoff retry. mediaId={}, retryCount={}, nextRetryAt={}",
                media.getId(),
                nextRetryCount,
                media.getNextRetryAt()
        );

        return jobCardMediaRepository.save(media);
    }

    @Override
    @Transactional
    public int wakeAuthRequiredRows() {

        int updated = jobCardMediaRepository.wakeAuthRequiredRows(LocalDateTime.now());

        if (updated > 0) {

            log.info(
                    "[MEDIA_RETRY] Woke {} AUTH_REQUIRED media row(s) back to PENDING after reauthorization.",
                    updated
            );
        }

        return updated;
    }

    /**
     * Folder for locally-buffered bytes pending a Drive attempt, matching
     * the same {@code garageos.media.storage-path}-rooted layout
     * {@link com.garageos.modules.navigation.storage.LocalMediaStorageService}
     * already uses for trip media, under its own subfolder.
     */
    public static String pendingUploadFolder() {
        return PENDING_UPLOAD_FOLDER;
    }
}
