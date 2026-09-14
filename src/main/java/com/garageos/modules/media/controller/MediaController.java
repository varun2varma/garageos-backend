package com.garageos.modules.media.controller;

import com.garageos.core.enums.media.MediaStage;
import com.garageos.modules.media.entity.JobCardMedia;
import com.garageos.modules.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/job-cards")
@RequiredArgsConstructor
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
            @RequestParam("file") MultipartFile file) {

        JobCardMedia media =
                mediaService.uploadMedia(
                        jobCardId,
                        stage,
                        file
                );

        return ResponseEntity.ok(media);
    }
}