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

        if (jobCardId == null) {
            throw new IllegalArgumentException("Job card id is required.");
        }

        if (mediaStage == null) {
            throw new IllegalArgumentException("Media stage is required.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required.");
        }

        JobCard jobCard =
                jobCardRepository.findById(jobCardId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Job Card not found with id : "
                                                + jobCardId));

        /*
         * -------------------------------------------------------------
         * GARAGE ISOLATION
         * -------------------------------------------------------------
         *
         * A logged-in user can only upload media against a JobCard
         * belonging to the user's current garage.
         */
        GarageUserPrincipal principal =
                (GarageUserPrincipal)
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getPrincipal();

        Long userGarageId = principal.getGarageId();

        if (userGarageId == null) {
            throw new IllegalStateException(
                    "User is not associated with a garage.");
        }

        if (jobCard.getGarage() == null) {
            throw new IllegalStateException(
                    "Job Card is not associated with a garage.");
        }

        Long jobCardGarageId =
                jobCard.getGarage().getId();

        if (!userGarageId.equals(jobCardGarageId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have access to this Job Card.");
        }

        String contentType = file.getContentType();

        MediaType mediaType =
                resolveMediaType(contentType);

        String extension =
                resolveExtension(
                        file.getOriginalFilename(),
                        contentType);

        int sequence =
                getNextSequence(
                        jobCardId,
                        mediaStage);

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

        try {

            File driveFolder =
                    folderService.getOrCreateStageFolder(
                            garageCode,
                            jobCardNumber,
                            mediaStage.name()
                    );

            File driveFile =
                    googleDriveFileService.uploadFile(
                            file,
                            generatedFileName,
                            driveFolder.getId()
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

            return jobCardMediaRepository.save(media);

        } catch (GeneralSecurityException | IOException ex) {

            throw new IllegalStateException(
                    "Failed to upload media to Google Drive.",
                    ex
            );
        }
    }

    private MediaType resolveMediaType(String contentType) {

        if (contentType == null || contentType.isBlank()) {
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
                                sequence);
            }
        }

        return maxSequence + 1;
    }
}