package com.garageos.modules.media.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.media.dto.request.UploadCompleteRequest;
import com.garageos.modules.media.dto.response.JobCardMediaResponse;
import com.garageos.modules.media.dto.response.MediaAccessResponse;
import com.garageos.modules.media.entity.JobCardMedia;
import com.garageos.modules.media.mapper.JobCardMediaMapper;
import com.garageos.modules.media.service.MediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Media-id-scoped endpoints for the direct-upload flow's second/third steps
 * (complete, access) plus deletion — flatter than {@link MediaController}'s
 * job-card-scoped paths (which own creation/listing/legacy upload), matching
 * this feature's own API design (media identity, once assigned, is enough
 * to address these operations; jobCardId is resolved internally from the
 * media row for authorization, the same defense-in-depth pattern
 * MediaServiceImpl already uses elsewhere).
 */
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Slf4j
public class MediaAccessController {

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

    /** Deliberately excludes TECHNICIAN — deletion is a privileged-employee action, same gate as visibility updates. */
    private static final String DELETE_ROLES = """
            hasAnyRole(
                'MANAGER',
                'SERVICE_ADVISOR',
                'OWNER'
            )
            """;

    @PostMapping("/{mediaId}/complete")
    @PreAuthorize(MEDIA_ROLES)
    public ResponseEntity<ApiResponse<JobCardMediaResponse>> completeUpload(
            @PathVariable Long mediaId,
            @Valid @RequestBody UploadCompleteRequest request) {

        log.info(
                "[MEDIA] Upload completion reported. mediaId={}, uploadSessionId={}",
                mediaId,
                request.getUploadSessionId()
        );

        JobCardMedia media = mediaService.completeUpload(mediaId, request);

        return ApiResponseUtil.success(
                "Upload completed.",
                mediaMapper.toResponse(media)
        );
    }

    @GetMapping("/{mediaId}/access")
    @PreAuthorize(MEDIA_ROLES)
    public ResponseEntity<ApiResponse<MediaAccessResponse>> getAccess(
            @PathVariable Long mediaId,
            @RequestParam(defaultValue = "original") String variant) {

        MediaAccessResponse access = mediaService.getPlaybackAccess(mediaId, variant);

        return ApiResponseUtil.success(
                "Media access granted.",
                access
        );
    }

    @DeleteMapping("/{mediaId}")
    @PreAuthorize(DELETE_ROLES)
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @PathVariable Long mediaId) {

        log.info("[MEDIA] Delete requested. mediaId={}", mediaId);

        mediaService.deleteMedia(mediaId);

        return ApiResponseUtil.success("Media deleted successfully.");
    }
}
