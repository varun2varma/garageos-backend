package com.garageos.modules.media.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.core.enums.media.MediaStage;
import com.garageos.modules.media.dto.request.UpdateMediaVisibilityRequest;
import com.garageos.modules.media.dto.request.UploadIntentRequest;
import com.garageos.modules.media.dto.response.JobCardMediaResponse;
import com.garageos.modules.media.dto.response.UploadIntentResponse;
import com.garageos.modules.media.entity.JobCardMedia;
import com.garageos.modules.media.mapper.JobCardMediaMapper;
import com.garageos.modules.media.service.MediaContent;
import com.garageos.modules.media.service.MediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Job-card-scoped media: upload (existing) plus listing and authenticated
 * content retrieval (new — see the Media feature design audit). Every
 * endpoint here shares the same role gate at the controller level;
 * finer-grained authorization (garage isolation, technician assignment)
 * happens in {@link MediaService}, the same way the original upload
 * endpoint already enforced garage isolation.
 */
@RestController
@RequestMapping("/api/v1/job-cards")
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final MediaService mediaService;
    private final JobCardMediaMapper mediaMapper;

    private static final String MEDIA_ROLES = """
            hasAnyRole(
                'MANAGER',
                'SERVICE_ADVISOR',
                'TECHNICIAN',
                'OWNER'
            )
            """;

    /**
     * Deliberately excludes TECHNICIAN — visibility correction is a
     * privileged-employee action, not part of the technician upload/view
     * flow. See MediaService.updateVisibility.
     */
    private static final String VISIBILITY_UPDATE_ROLES = """
            hasAnyRole(
                'MANAGER',
                'SERVICE_ADVISOR',
                'OWNER'
            )
            """;

    @PostMapping("/{jobCardId}/media")
    @PreAuthorize(MEDIA_ROLES)
    public ResponseEntity<JobCardMedia> uploadMedia(
            @PathVariable Long jobCardId,
            @RequestParam("stage") MediaStage stage,
            @RequestParam(value = "repairTaskId", required = false)
            Long repairTaskId,
            @RequestParam("file") MultipartFile file) {

        log.info(
                "[MEDIA] Upload request received. jobCardId={}, stage={}, repairTaskId={}, fileName={}, contentType={}, size={}",
                jobCardId,
                stage,
                repairTaskId,
                file != null ? file.getOriginalFilename() : null,
                file != null ? file.getContentType() : null,
                file != null ? file.getSize() : null
        );

        try {

            JobCardMedia media =
                    mediaService.uploadMedia(
                            jobCardId,
                            stage,
                            repairTaskId,
                            file
                    );

            log.info(
                    "[MEDIA] Upload request completed successfully. jobCardId={}, mediaId={}, fileName={}",
                    jobCardId,
                    media.getId(),
                    media.getFileName()
            );

            return ResponseEntity.ok(media);

        } catch (Exception ex) {

            log.error(
                    "[MEDIA] Upload request failed. jobCardId={}, stage={}, repairTaskId={}, error={}",
                    jobCardId,
                    stage,
                    repairTaskId,
                    ex.getMessage(),
                    ex
            );

            throw ex;
        }
    }

    /**
     * Step 1 of the direct-to-R2 upload flow (see MediaServiceImpl's own
     * doc comments). Returns MEDIA_STORAGE_NOT_CONFIGURED (503) if R2 isn't
     * provisioned yet — callers should fall back to the legacy
     * {@code POST /{jobCardId}/media} endpoint above in that case.
     */
    @PostMapping("/{jobCardId}/media/upload-intent")
    @PreAuthorize(MEDIA_ROLES)
    public ResponseEntity<ApiResponse<UploadIntentResponse>> createUploadIntent(
            @PathVariable Long jobCardId,
            @Valid @RequestBody UploadIntentRequest request) {

        log.info(
                "[MEDIA] Upload intent requested. jobCardId={}, stage={}, contentType={}, fileSize={}",
                jobCardId,
                request.getStage(),
                request.getContentType(),
                request.getFileSize()
        );

        UploadIntentResponse response =
                mediaService.createUploadIntent(jobCardId, request);

        return ApiResponseUtil.created(
                "Upload authorized.",
                response
        );
    }

    /**
     * Employee/technician/owner-side listing — returns the richer
     * {@link com.garageos.modules.media.dto.response.JobCardMediaAuditResponse}
     * (includes WHO/WHEN/WHERE capture metadata). The customer-portal
     * listing endpoint (CustomerPortalController) is a separate endpoint
     * entirely and still returns the plain {@link JobCardMediaResponse} —
     * see JobCardMediaAuditResponse's own doc comment for why that's a
     * deliberate privacy decision, not an oversight.
     */
    @GetMapping("/{jobCardId}/media")
    @PreAuthorize(MEDIA_ROLES)
    public ResponseEntity<ApiResponse<List<com.garageos.modules.media.dto.response.JobCardMediaAuditResponse>>> listMedia(
            @PathVariable Long jobCardId) {

        log.info(
                "[MEDIA] List request received. jobCardId={}",
                jobCardId
        );

        List<JobCardMedia> media =
                mediaService.listMedia(jobCardId);

        return ApiResponseUtil.success(
                "Media fetched successfully.",
                mediaMapper.toAuditResponseList(media)
        );
    }

    @GetMapping("/{jobCardId}/media/{mediaId}/content")
    @PreAuthorize(MEDIA_ROLES)
    public ResponseEntity<byte[]> getMediaContent(
            @PathVariable Long jobCardId,
            @PathVariable Long mediaId) {

        log.info(
                "[MEDIA] Content request received. jobCardId={}, mediaId={}",
                jobCardId,
                mediaId
        );

        MediaContent content =
                mediaService.getMediaContent(jobCardId, mediaId);

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                content.contentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + content.fileName() + "\"")
                .body(content.content());
    }

    @PutMapping("/{jobCardId}/media/{mediaId}/visibility")
    @PreAuthorize(VISIBILITY_UPDATE_ROLES)
    public ResponseEntity<ApiResponse<JobCardMediaResponse>> updateMediaVisibility(
            @PathVariable Long jobCardId,
            @PathVariable Long mediaId,
            @Valid @RequestBody UpdateMediaVisibilityRequest request) {

        log.info(
                "[MEDIA] Visibility update request received. jobCardId={}, mediaId={}, visibility={}",
                jobCardId,
                mediaId,
                request.getVisibility()
        );

        JobCardMedia media =
                mediaService.updateVisibility(
                        jobCardId,
                        mediaId,
                        request.getVisibility()
                );

        return ApiResponseUtil.success(
                "Media visibility updated successfully.",
                mediaMapper.toResponse(media)
        );
    }
}