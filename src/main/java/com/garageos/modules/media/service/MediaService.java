package com.garageos.modules.media.service;

import com.garageos.modules.media.entity.JobCardMedia;
import com.garageos.core.enums.media.MediaStage;
import com.garageos.core.enums.media.MediaVisibility;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaService {

    JobCardMedia uploadMedia(
            Long jobCardId,
            MediaStage mediaStage,
            Long repairTaskId,
            MultipartFile file
    );

    /**
     * Employee/technician/owner-side listing. Authorizes the currently
     * authenticated principal (garage isolation, plus a JobAssignment check
     * when the caller is a technician) internally, the same way
     * {@link #uploadMedia} already does.
     */
    List<JobCardMedia> listMedia(Long jobCardId);

    /**
     * Employee/technician/owner-side content download. Authorizes the same
     * way as {@link #listMedia}, then fetches the bytes from storage.
     */
    MediaContent getMediaContent(Long jobCardId, Long mediaId);

    /**
     * Pure storage fetch: downloads the bytes for an already-resolved,
     * already-authorized {@link JobCardMedia} row. Does not perform any
     * authorization itself — callers (this service's own employee-side
     * methods, or the customer portal after its own ownership check) are
     * responsible for authorizing access to {@code media} first.
     */
    MediaContent downloadContent(JobCardMedia media);

    /**
     * Visibility-correction: OWNER/MANAGER/SERVICE_ADVISOR only (enforced
     * by the controller's {@code @PreAuthorize} — TECHNICIAN never reaches
     * this method). Reuses the same garage-isolation check every other
     * employee-side media operation uses; does not touch the stage-driven
     * initial-visibility logic in {@link #uploadMedia} at all.
     */
    JobCardMedia updateVisibility(Long jobCardId, Long mediaId, MediaVisibility visibility);
}