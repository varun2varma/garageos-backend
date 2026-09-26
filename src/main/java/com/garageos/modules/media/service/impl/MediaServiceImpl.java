package com.garageos.modules.media.service.impl;

import com.garageos.core.enums.JobAssignmentStatus;
import com.garageos.core.enums.media.MediaStage;
import com.garageos.core.enums.media.MediaType;
import com.garageos.core.enums.media.MediaVisibility;
import com.garageos.core.exception.MediaException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.jobassignment.entity.JobAssignment;
import com.garageos.modules.jobassignment.repository.JobAssignmentRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.media.entity.JobCardMedia;
import com.garageos.modules.media.repository.JobCardMediaRepository;
import com.garageos.modules.media.service.GoogleDriveFileService;
import com.garageos.modules.media.service.MediaContent;
import com.garageos.modules.media.service.MediaService;
import com.garageos.modules.media.service.MediaUploadRetryService;
import com.garageos.modules.navigation.storage.MediaStorageService;
import com.garageos.modules.repairtask.entity.RepairTask;
import com.garageos.modules.repairtask.repository.RepairTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaServiceImpl implements MediaService {

    private static final Pattern SEQUENCE_PATTERN =
            Pattern.compile("_(\\d+)\\.[^.]+$");

    private static final String VISIBILITY_INTERNAL =
            "INTERNAL";

    private static final String VISIBILITY_CUSTOMER_VISIBLE =
            "CUSTOMER_VISIBLE";

    private static final String PENDING_UPLOAD_FOLDER = "job-card-media/pending";

    private final JobCardRepository jobCardRepository;
    private final JobCardMediaRepository jobCardMediaRepository;
    private final RepairTaskRepository repairTaskRepository;
    private final JobAssignmentRepository jobAssignmentRepository;
    private final GoogleDriveFileService googleDriveFileService;
    private final MediaUploadRetryService mediaUploadRetryService;
    private final MediaStorageService mediaStorageService;

    @Override
    public JobCardMedia uploadMedia(
            Long jobCardId,
            MediaStage mediaStage,
            Long repairTaskId,
            MultipartFile file) {

        log.info(
                "[MEDIA] Starting media upload. jobCardId={}, stage={}, repairTaskId={}",
                jobCardId,
                mediaStage,
                repairTaskId
        );

        if (jobCardId == null) {

            log.warn("[MEDIA] Job card id is missing.");

            throw new IllegalArgumentException(
                    "Job card id is required."
            );
        }

        if (mediaStage == null) {

            log.warn(
                    "[MEDIA] Media stage is missing. jobCardId={}",
                    jobCardId
            );

            throw new IllegalArgumentException(
                    "Media stage is required."
            );
        }

        if (file == null || file.isEmpty()) {

            log.warn(
                    "[MEDIA] File is missing or empty. jobCardId={}, stage={}",
                    jobCardId,
                    mediaStage
            );

            throw new IllegalArgumentException(
                    "File is required."
            );
        }

        validateRepairTaskRequirement(
                jobCardId,
                mediaStage,
                repairTaskId
        );

        log.info(
                "[MEDIA] File validated. jobCardId={}, stage={}, repairTaskId={}, fileName={}, contentType={}, size={}",
                jobCardId,
                mediaStage,
                repairTaskId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );

        JobCard jobCard =
                jobCardRepository.findById(jobCardId)
                        .orElseThrow(() -> {

                            log.warn(
                                    "[MEDIA] Job Card not found. jobCardId={}",
                                    jobCardId
                            );

                            return new ResourceNotFoundException(
                                    "Job Card not found with id : "
                                            + jobCardId
                            );
                        });

        log.info(
                "[MEDIA] Job Card loaded. jobCardId={}, jobCardNumber={}",
                jobCardId,
                jobCard.getJobCardNumber()
        );

        /*
         * -------------------------------------------------------------
         * AUTHORIZATION (garage isolation + technician assignment)
         * -------------------------------------------------------------
         */

        GarageUserPrincipal principal =
                (GarageUserPrincipal)
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getPrincipal();

        authorizeEmployeeAccess(jobCard, principal);

        /*
         * -------------------------------------------------------------
         * REPAIR TASK VALIDATION
         * -------------------------------------------------------------
         */

        RepairTask repairTask = null;

        if (repairTaskId != null) {

            repairTask =
                    repairTaskRepository.findById(repairTaskId)
                            .orElseThrow(() -> {

                                log.warn(
                                        "[MEDIA] Repair Task not found. repairTaskId={}, jobCardId={}",
                                        repairTaskId,
                                        jobCardId
                                );

                                return new ResourceNotFoundException(
                                        "Repair Task not found with id : "
                                                + repairTaskId
                                );
                            });

            if (repairTask.getJobCard() == null
                    || repairTask.getJobCard().getId() == null
                    || !jobCardId.equals(
                    repairTask.getJobCard().getId())) {

                log.warn(
                        "[MEDIA] Repair Task does not belong to Job Card. repairTaskId={}, jobCardId={}",
                        repairTaskId,
                        jobCardId
                );

                throw new IllegalArgumentException(
                        "Repair Task does not belong to this Job Card."
                );
            }

            log.info(
                    "[MEDIA] Repair Task validated. repairTaskId={}, jobCardId={}",
                    repairTaskId,
                    jobCardId
            );
        }

        String contentType =
                file.getContentType();

        MediaType mediaType =
                resolveMediaType(contentType);

        log.info(
                "[MEDIA] Media type resolved. jobCardId={}, mediaType={}, contentType={}",
                jobCardId,
                mediaType,
                contentType
        );

        String extension =
                resolveExtension(
                        file.getOriginalFilename(),
                        contentType
                );

        log.info(
                "[MEDIA] File extension resolved. jobCardId={}, extension={}",
                jobCardId,
                extension
        );

        int sequence =
                getNextSequence(
                        jobCardId,
                        mediaStage
                );

        log.info(
                "[MEDIA] Media sequence resolved. jobCardId={}, stage={}, sequence={}",
                jobCardId,
                mediaStage,
                sequence
        );

        String jobCardNumber =
                jobCard.getJobCardNumber();

        String generatedFileName =
                String.format(
                        "%s_%s_%03d.%s",
                        jobCardNumber,
                        mediaStage.name(),
                        sequence,
                        extension
                );

        log.info(
                "[MEDIA] Generated Drive file name. jobCardId={}, fileName={}",
                jobCardId,
                generatedFileName
        );

        String visibility =
                resolveInitialVisibility(
                        mediaStage
                );

        log.info(
                "[MEDIA] Initial visibility resolved. jobCardId={}, stage={}, visibility={}",
                jobCardId,
                mediaStage,
                visibility
        );

        /*
         * -------------------------------------------------------------
         * DURABLE ACCEPTANCE (fix for symptom (B))
         * -------------------------------------------------------------
         * The bytes are buffered to local storage and the JobCardMedia row
         * is created and saved BEFORE any Drive call is attempted. This is
         * the actual fix for "no error, but the media doesn't reliably end
         * up saved/visible": previously nothing was persisted until AFTER
         * the Drive upload had already fully succeeded, so a transient
         * Drive failure, or the client's connection dropping mid-upload on
         * a large before/after video over a mobile network, left no
         * durable record of the attempt at all - the request simply failed
         * (or never got an acknowledged response) and the media was gone.
         * From this point on, a failure is recoverable: the bytes and a
         * row exist, and the retry queue (MediaUploadRetryService /
         * MediaUploadRetryScheduler) owns getting it into Drive.
         */

        String localStorageKey =
                mediaStorageService.upload(
                        file,
                        PENDING_UPLOAD_FOLDER
                );

        log.info(
                "[MEDIA] File buffered locally pending Drive upload. jobCardId={}, localStorageKey={}",
                jobCardId,
                localStorageKey
        );

        JobCardMedia media =
                JobCardMedia.builder()
                        .jobCardId(jobCardId)
                        .repairTaskId(repairTaskId)
                        .fileName(generatedFileName)
                        .mediaType(mediaType.name())
                        .mediaStage(mediaStage.name())
                        .contentType(contentType)
                        .fileSize(file.getSize())
                        .uploadedBy(principal.getId())
                        .visibility(visibility)
                        .createdAt(LocalDateTime.now())
                        .uploadStatus("PENDING")
                        .retryCount(0)
                        // Set even though the synchronous attempt below
                        // runs immediately: if the process dies between
                        // this save and that attempt, the row is still
                        // immediately eligible for the backoff scheduler's
                        // findByUploadStatusInAndNextRetryAtLessThanEqual
                        // scan instead of being stuck with a null
                        // nextRetryAt that query would never match.
                        .nextRetryAt(LocalDateTime.now())
                        .localStoragePath(localStorageKey)
                        .build();

        JobCardMedia savedMedia =
                jobCardMediaRepository.save(media);

        log.info(
                "[MEDIA] Media row durably accepted. mediaId={}, jobCardId={}, repairTaskId={}, visibility={}",
                savedMedia.getId(),
                jobCardId,
                repairTaskId,
                visibility
        );

        /*
         * -------------------------------------------------------------
         * FIRST DRIVE ATTEMPT (same request, same code path the backoff
         * retry job later reuses via MediaUploadRetryService)
         * -------------------------------------------------------------
         */

        JobCardMedia afterAttempt =
                mediaUploadRetryService.attemptUpload(savedMedia);

        return switch (afterAttempt.getUploadStatus()) {

            case "COMPLETED" -> afterAttempt;

            case "AUTH_REQUIRED" -> throw new MediaException(
                    MediaException.MediaErrorCode.MEDIA_DRIVE_AUTH_FAILED,
                    "Google Drive rejected the stored authorization. "
                            + "Reauthorize Google Drive and try again. "
                            + "This media has been saved and will be uploaded "
                            + "automatically once Drive is reauthorized.",
                    null
            );

            case "RETRY_WAIT" -> throw new MediaException(
                    MediaException.MediaErrorCode.MEDIA_UPLOAD_RETRY_SCHEDULED,
                    "Google Drive could not be reached right now. This media "
                            + "has been saved and will be uploaded automatically.",
                    null
            );

            default -> throw new MediaException(
                    MediaException.MediaErrorCode.MEDIA_DRIVE_UPLOAD_FAILED,
                    "Google Drive could not store this file.",
                    null
            );
        };
    }

    /**
     * Turns a Google failure into something the client can act on.
     *
     * Every Drive failure previously became one IllegalStateException
     * with the message "Failed to upload media to Google Drive." - which
     * the handler returned as a 400. That was wrong twice over: the cause
     * was unknowable to the caller, and a Drive outage or an expired
     * token is not a client error.
     *
     * The distinction that matters operationally is authorization (someone
     * must reauthorize Drive) versus anything else (retry, or investigate
     * the logs). Google signals the former with 401/403 on the API call,
     * and with TokenResponseException when a refresh is refused.
     *
     * The exception's own text is never put in the client message -
     * Google's errors can include request URLs and token metadata. The
     * full cause goes to the logs above and is attached for the handler.
     */
    private MediaException toMediaException(Exception ex) {

        if (ex instanceof TokenResponseException) {

            return new MediaException(
                    MediaException.MediaErrorCode.MEDIA_DRIVE_AUTH_FAILED,
                    "Google Drive rejected the stored authorization. "
                            + "Reauthorize Google Drive and try again.",
                    ex
            );
        }

        if (ex instanceof GoogleJsonResponseException googleError) {

            int statusCode = googleError.getStatusCode();

            if (statusCode == 401 || statusCode == 403) {

                return new MediaException(
                        MediaException.MediaErrorCode.MEDIA_DRIVE_AUTH_FAILED,
                        "Google Drive refused this operation. The stored "
                                + "authorization may have expired or lost access.",
                        ex
                );
            }
        }

        return new MediaException(
                MediaException.MediaErrorCode.MEDIA_DRIVE_UPLOAD_FAILED,
                "Google Drive could not store this file. Please try again.",
                ex
        );
    }

    @Override
    public List<JobCardMedia> listMedia(Long jobCardId) {

        log.info(
                "[MEDIA] Listing media. jobCardId={}",
                jobCardId
        );

        JobCard jobCard =
                jobCardRepository.findById(jobCardId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Job Card not found with id : "
                                                + jobCardId));

        GarageUserPrincipal principal =
                (GarageUserPrincipal)
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getPrincipal();

        authorizeEmployeeAccess(jobCard, principal);

        return jobCardMediaRepository
                .findByJobCardIdOrderByCreatedAtAsc(jobCardId);
    }

    @Override
    public MediaContent getMediaContent(Long jobCardId, Long mediaId) {

        log.info(
                "[MEDIA] Fetching media content. jobCardId={}, mediaId={}",
                jobCardId,
                mediaId
        );

        JobCard jobCard =
                jobCardRepository.findById(jobCardId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Job Card not found with id : "
                                                + jobCardId));

        GarageUserPrincipal principal =
                (GarageUserPrincipal)
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getPrincipal();

        authorizeEmployeeAccess(jobCard, principal);

        JobCardMedia media =
                jobCardMediaRepository.findById(mediaId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Media not found with id : "
                                                + mediaId));

        // Defense in depth: never trust that mediaId alone belongs to
        // jobCardId just because both path variables were supplied.
        if (!jobCardId.equals(media.getJobCardId())) {

            log.warn(
                    "[MEDIA] Media does not belong to Job Card. mediaId={}, jobCardId={}, actualJobCardId={}",
                    mediaId,
                    jobCardId,
                    media.getJobCardId()
            );

            throw new ResourceNotFoundException(
                    "Media not found with id : " + mediaId
            );
        }

        return downloadContent(media);
    }

    @Override
    public JobCardMedia updateVisibility(
            Long jobCardId,
            Long mediaId,
            MediaVisibility visibility) {

        log.info(
                "[MEDIA] Visibility update requested. jobCardId={}, mediaId={}, visibility={}",
                jobCardId,
                mediaId,
                visibility
        );

        JobCard jobCard =
                jobCardRepository.findById(jobCardId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Job Card not found with id : "
                                                + jobCardId));

        GarageUserPrincipal principal =
                (GarageUserPrincipal)
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getPrincipal();

        // Reuses the same garage-isolation check every other employee-side
        // media operation uses. The technician-assignment branch inside it
        // is unreachable here in practice — the controller's @PreAuthorize
        // restricts this endpoint to MANAGER/SERVICE_ADVISOR/OWNER, all of
        // which take the "privileged" (garage-only) path — but calling the
        // same shared method keeps this one authorization rule in one
        // place rather than re-implementing the garage check a third time.
        authorizeEmployeeAccess(jobCard, principal);

        JobCardMedia media =
                jobCardMediaRepository.findById(mediaId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Media not found with id : "
                                                + mediaId));

        // Defense in depth, same as getMediaContent: never trust that
        // mediaId alone belongs to jobCardId just because both path
        // variables were supplied.
        if (!jobCardId.equals(media.getJobCardId())) {

            log.warn(
                    "[MEDIA] Media does not belong to Job Card. mediaId={}, jobCardId={}, actualJobCardId={}",
                    mediaId,
                    jobCardId,
                    media.getJobCardId()
            );

            throw new ResourceNotFoundException(
                    "Media not found with id : " + mediaId
            );
        }

        media.setVisibility(visibility.name());

        JobCardMedia saved =
                jobCardMediaRepository.save(media);

        log.info(
                "[MEDIA] Visibility updated. mediaId={}, jobCardId={}, visibility={}",
                mediaId,
                jobCardId,
                visibility
        );

        return saved;
    }

    @Override
    public MediaContent downloadContent(JobCardMedia media) {

        try {

            byte[] content =
                    googleDriveFileService.downloadFile(
                            media.getDriveFileId()
                    );

            return new MediaContent(
                    content,
                    media.getContentType(),
                    media.getFileName()
            );

        } catch (GeneralSecurityException | IOException ex) {

            log.error(
                    "[DRIVE] Failed to download media content. mediaId={}, driveFileId={}, error={}",
                    media.getId(),
                    media.getDriveFileId(),
                    ex.getMessage(),
                    ex
            );

            // Same mapping uploadMedia() already uses (toMediaException) —
            // previously this collapsed a Drive-auth failure (expired/
            // revoked token, 401/403) into a generic IllegalStateException
            // that GlobalExceptionHandler flattened to a plain 400, hiding
            // that the real fix is reauthorization, not retrying the
            // request. Routing it through the same classifier means an
            // auth failure now correctly surfaces as 503
            // MEDIA_DRIVE_AUTH_FAILED on download exactly like it already
            // does on upload.
            throw toMediaException(ex);
        }
    }

    /**
     * Shared authorization for every employee/technician/owner-side media
     * operation (upload, list, content download):
     *   - the caller must belong to the same garage as the Job Card
     *     (identical check/messages to the original upload-only garage
     *     isolation check this was extracted from);
     *   - a caller whose only relevant role is TECHNICIAN must additionally
     *     hold a non-cancelled {@link JobAssignment} on this Job Card —
     *     using the existing JobAssignment mechanism, per the Media feature
     *     requirement that technicians only reach jobs they're assigned to.
     * MANAGER/SERVICE_ADVISOR/OWNER are not restricted beyond garage
     * isolation, matching how those roles are treated everywhere else in
     * this backend (no other endpoint restricts them to a subset of their
     * garage's job cards).
     */
    private void authorizeEmployeeAccess(
            JobCard jobCard,
            GarageUserPrincipal principal) {

        Long userGarageId =
                principal.getGarageId();

        log.info(
                "[MEDIA] Garage context resolved. jobCardId={}, userGarageId={}",
                jobCard.getId(),
                userGarageId
        );

        if (userGarageId == null) {

            log.error(
                    "[MEDIA] User is not associated with a garage. jobCardId={}",
                    jobCard.getId()
            );

            throw new IllegalStateException(
                    "User is not associated with a garage."
            );
        }

        if (jobCard.getGarage() == null) {

            log.error(
                    "[MEDIA] Job Card is not associated with a garage. jobCardId={}",
                    jobCard.getId()
            );

            throw new IllegalStateException(
                    "Job Card is not associated with a garage."
            );
        }

        Long jobCardGarageId =
                jobCard.getGarage().getId();

        log.info(
                "[MEDIA] Garage isolation check. jobCardId={}, userGarageId={}, jobCardGarageId={}",
                jobCard.getId(),
                userGarageId,
                jobCardGarageId
        );

        if (!userGarageId.equals(jobCardGarageId)) {

            log.warn(
                    "[MEDIA] Garage access denied. jobCardId={}, userGarageId={}, jobCardGarageId={}",
                    jobCard.getId(),
                    userGarageId,
                    jobCardGarageId
            );

            throw new AccessDeniedException(
                    "You do not have access to this Job Card."
            );
        }

        log.info(
                "[MEDIA] Garage access validated. jobCardId={}, garageId={}",
                jobCard.getId(),
                jobCardGarageId
        );

        Set<String> roles =
                principal.getRoles();

        boolean privileged =
                roles.contains("MANAGER")
                        || roles.contains("SERVICE_ADVISOR")
                        || roles.contains("OWNER");

        if (privileged) {
            return;
        }

        if (roles.contains("TECHNICIAN")) {

            boolean assigned =
                    jobAssignmentRepository
                            .findByJobCardId(jobCard.getId())
                            .stream()
                            .anyMatch(assignment ->
                                    assignment.getUser() != null
                                            && assignment.getUser().getId() != null
                                            && assignment.getUser().getId().equals(principal.getId())
                                            && assignment.getStatus() != JobAssignmentStatus.CANCELLED);

            if (!assigned) {

                log.warn(
                        "[MEDIA] Technician has no assignment on this Job Card. jobCardId={}, userId={}",
                        jobCard.getId(),
                        principal.getId()
                );

                throw new AccessDeniedException(
                        "You are not assigned to this Job Card."
                );
            }

            log.info(
                    "[MEDIA] Technician assignment validated. jobCardId={}, userId={}",
                    jobCard.getId(),
                    principal.getId()
            );

            return;
        }

        log.warn(
                "[MEDIA] Caller has no role entitled to this Job Card's media. jobCardId={}, roles={}",
                jobCard.getId(),
                roles
        );

        throw new AccessDeniedException(
                "You do not have access to this Job Card's media."
        );
    }

    /**
     * Corrective fix: DURING_REPAIR media used to REQUIRE a repairTaskId.
     * No client ever sent one - the Job Card screen uploads against the
     * job card and a stage, which is the only thing it knows - so every
     * repair photo and video was rejected before it reached Google Drive.
     * The rejection was an IllegalArgumentException, which had no handler
     * and so surfaced as a bare 500 "Something went wrong."; that is the
     * whole of the "repair media upload returns 500" defect, and it is
     * also why the Photos & Videos section stayed empty.
     *
     * Repair media is now valid at job-card level. A repairTaskId remains
     * optional and, when supplied, still narrows the media to that one
     * task - so per-task media keeps working without making it the only
     * way to upload.
     *
     * Note what is NOT required here: a task name, a job card number, a
     * folder name or any other display string. Media is identified by
     * stable ids (jobCardId, optional repairTaskId) plus the stage, so a
     * task with an arbitrary, duplicated, renamed or empty name cannot
     * break an upload.
     */
    private void validateRepairTaskRequirement(
            Long jobCardId,
            MediaStage mediaStage,
            Long repairTaskId) {

        if (mediaStage != MediaStage.DURING_REPAIR
                && repairTaskId != null) {

            log.warn(
                    "[MEDIA] Repair Task is only allowed for DURING_REPAIR media. jobCardId={}, stage={}, repairTaskId={}",
                    jobCardId,
                    mediaStage,
                    repairTaskId
            );

            throw new IllegalArgumentException(
                    "Repair Task can only be specified for DURING_REPAIR media."
            );
        }
    }

    private String resolveInitialVisibility(
            MediaStage mediaStage) {

        if (mediaStage == MediaStage.DURING_REPAIR) {
            return VISIBILITY_INTERNAL;
        }

        return VISIBILITY_CUSTOMER_VISIBLE;
    }

    private MediaType resolveMediaType(
            String contentType) {

        if (contentType == null || contentType.isBlank()) {

            log.warn(
                    "[MEDIA] File content type is missing."
            );

            throw new IllegalArgumentException(
                    "File content type is required."
            );
        }

        if (contentType
                .toLowerCase()
                .startsWith("image/")) {

            return MediaType.IMAGE;
        }

        if (contentType
                .toLowerCase()
                .startsWith("video/")) {

            return MediaType.VIDEO;
        }

        log.warn(
                "[MEDIA] Unsupported content type: {}",
                contentType
        );

        throw new IllegalArgumentException(
                "Only image and video files are supported."
        );
    }

    private String resolveExtension(
            String originalFileName,
            String contentType) {

        if (originalFileName != null) {

            int dotIndex =
                    originalFileName.lastIndexOf('.');

            if (dotIndex >= 0
                    && dotIndex < originalFileName.length() - 1) {

                return originalFileName
                        .substring(dotIndex + 1)
                        .toLowerCase();
            }
        }

        return switch (contentType.toLowerCase()) {

            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "video/mp4" -> "mp4";
            case "video/webm" -> "webm";
            case "video/quicktime" -> "mov";

            default ->
                    throw new IllegalArgumentException(
                            "Unable to determine file extension."
                    );
        };
    }

    private int getNextSequence(
            Long jobCardId,
            MediaStage mediaStage) {

        log.debug(
                "[MEDIA] Calculating next sequence. jobCardId={}, stage={}",
                jobCardId,
                mediaStage
        );

        List<JobCardMedia> existingMedia =
                jobCardMediaRepository
                        .findByJobCardIdAndMediaStageOrderByCreatedAtAsc(
                                jobCardId,
                                mediaStage.name()
                        );

        int maxSequence = 0;

        for (JobCardMedia media : existingMedia) {

            if (media.getFileName() == null) {
                continue;
            }

            Matcher matcher =
                    SEQUENCE_PATTERN.matcher(
                            media.getFileName());

            if (matcher.find()) {

                int sequence =
                        Integer.parseInt(
                                matcher.group(1));

                maxSequence =
                        Math.max(
                                maxSequence,
                                sequence
                        );
            }
        }

        log.debug(
                "[MEDIA] Existing media count={}, maxSequence={}, nextSequence={}",
                existingMedia.size(),
                maxSequence,
                maxSequence + 1
        );

        return maxSequence + 1;
    }
}