package com.garageos.modules.media.service.impl;

import com.garageos.core.enums.media.MediaStage;
import com.garageos.core.enums.media.MediaType;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.media.entity.JobCardMedia;
import com.garageos.modules.media.repository.JobCardMediaRepository;
import com.garageos.modules.media.service.GoogleDriveFileService;
import com.garageos.modules.media.service.GoogleDriveFolderService;
import com.garageos.modules.media.service.MediaService;
import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaServiceImpl implements MediaService {

    private static final Pattern SEQUENCE_PATTERN =
            Pattern.compile("_(\\d+)\\.[^.]+$");

    private final JobCardRepository jobCardRepository;
    private final JobCardMediaRepository jobCardMediaRepository;
    private final GoogleDriveFolderService folderService;
    private final GoogleDriveFileService googleDriveFileService;

    @Override
    public JobCardMedia uploadMedia(
            Long jobCardId,
            MediaStage mediaStage,
            MultipartFile file) {

        log.info(
                "[MEDIA] Starting media upload. jobCardId={}, stage={}",
                jobCardId,
                mediaStage
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

        log.info(
                "[MEDIA] File validated. jobCardId={}, stage={}, fileName={}, contentType={}, size={}",
                jobCardId,
                mediaStage,
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
         * GARAGE ISOLATION
         * -------------------------------------------------------------
         */

        GarageUserPrincipal principal =
                (GarageUserPrincipal)
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getPrincipal();

        Long userGarageId = principal.getGarageId();

        log.info(
                "[MEDIA] Garage context resolved. jobCardId={}, userGarageId={}",
                jobCardId,
                userGarageId
        );

        if (userGarageId == null) {

            log.error(
                    "[MEDIA] User is not associated with a garage. jobCardId={}",
                    jobCardId
            );

            throw new IllegalStateException(
                    "User is not associated with a garage."
            );
        }

        if (jobCard.getGarage() == null) {

            log.error(
                    "[MEDIA] Job Card is not associated with a garage. jobCardId={}",
                    jobCardId
            );

            throw new IllegalStateException(
                    "Job Card is not associated with a garage."
            );
        }

        Long jobCardGarageId =
                jobCard.getGarage().getId();

        log.info(
                "[MEDIA] Garage isolation check. jobCardId={}, userGarageId={}, jobCardGarageId={}",
                jobCardId,
                userGarageId,
                jobCardGarageId
        );

        if (!userGarageId.equals(jobCardGarageId)) {

            log.warn(
                    "[MEDIA] Garage access denied. jobCardId={}, userGarageId={}, jobCardGarageId={}",
                    jobCardId,
                    userGarageId,
                    jobCardGarageId
            );

            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have access to this Job Card."
            );
        }

        log.info(
                "[MEDIA] Garage access validated. jobCardId={}, garageId={}",
                jobCardId,
                jobCardGarageId
        );

        String contentType = file.getContentType();

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

        String garageCode =
                jobCard.getGarage().getGarageCode();

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

        try {

            log.info(
                    "[DRIVE] Resolving stage folder. garageCode={}, jobCardNumber={}, stage={}",
                    garageCode,
                    jobCardNumber,
                    mediaStage
            );

            File driveFolder =
                    folderService.getOrCreateStageFolder(
                            garageCode,
                            jobCardNumber,
                            mediaStage.name()
                    );

            log.info(
                    "[DRIVE] Stage folder resolved. folderId={}, folderName={}",
                    driveFolder.getId(),
                    driveFolder.getName()
            );

            log.info(
                    "[DRIVE] Starting file upload. jobCardId={}, fileName={}, folderId={}",
                    jobCardId,
                    generatedFileName,
                    driveFolder.getId()
            );

            File driveFile =
                    googleDriveFileService.uploadFile(
                            file,
                            generatedFileName,
                            driveFolder.getId()
                    );

            log.info(
                    "[DRIVE] File upload successful. jobCardId={}, driveFileId={}, fileName={}",
                    jobCardId,
                    driveFile.getId(),
                    generatedFileName
            );

            JobCardMedia media =
                    JobCardMedia.builder()
                            .jobCardId(jobCardId)
                            .fileName(generatedFileName)
                            .driveFileId(driveFile.getId())
                            .driveWebViewLink(
                                    driveFile.getWebViewLink())
                            .mediaType(mediaType.name())
                            .mediaStage(mediaStage.name())
                            .contentType(contentType)
                            .fileSize(file.getSize())
                            .createdAt(LocalDateTime.now())
                            .build();

            log.info(
                    "[MEDIA] Saving media metadata to database. jobCardId={}, driveFileId={}",
                    jobCardId,
                    driveFile.getId()
            );

            JobCardMedia savedMedia =
                    jobCardMediaRepository.save(media);

            log.info(
                    "[MEDIA] Media metadata saved successfully. mediaId={}, jobCardId={}, driveFileId={}",
                    savedMedia.getId(),
                    jobCardId,
                    driveFile.getId()
            );

            return savedMedia;

        } catch (GeneralSecurityException | IOException ex) {

            log.error(
                    "[DRIVE] Google Drive operation failed. jobCardId={}, fileName={}, error={}",
                    jobCardId,
                    generatedFileName,
                    ex.getMessage(),
                    ex
            );

            throw new IllegalStateException(
                    "Failed to upload media to Google Drive.",
                    ex
            );
        }
    }

    private MediaType resolveMediaType(String contentType) {

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