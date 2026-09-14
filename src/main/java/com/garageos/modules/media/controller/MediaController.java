package com.garageos.modules.media.controller;

import com.garageos.core.enums.media.MediaStage;
import com.garageos.modules.media.entity.JobCardMedia;
import com.garageos.modules.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/job-cards")
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/{jobCardId}/media")
    @PreAuthorize("""
            hasAnyRole(
                'MANAGER',
                'SERVICE_ADVISOR',
                'TECHNICIAN'
            )
            """)
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
}