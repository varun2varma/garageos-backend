package com.garageos.modules.media.service;

import com.garageos.modules.media.dto.request.UploadIntentRequest;
import com.garageos.modules.media.dto.response.MediaAccessResponse;
import com.garageos.modules.media.dto.response.UploadIntentResponse;
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

    /**
     * Step 1 of the direct-to-R2 upload flow: authorizes the caller (same
     * garage-isolation + technician-assignment check as {@link #uploadMedia}),
     * reserves a {@link JobCardMedia} row (status UPLOADED-pending, i.e.
     * PENDING) with a generated, provider-neutral object key, and returns a
     * short-lived R2 upload authorization. Never used for GOOGLE_DRIVE —
     * that provider has no direct-upload capability, so this method throws
     * MEDIA_STORAGE_NOT_CONFIGURED if R2 isn't set up yet, telling the
     * client to fall back to the existing {@link #uploadMedia} endpoint.
     */
    UploadIntentResponse createUploadIntent(Long jobCardId, UploadIntentRequest request);

    /**
     * Step 2: the client reports it finished uploading directly to R2.
     * Idempotent by {@code uploadSessionId} — a retried call with the same
     * session id returns the already-completed row rather than reprocessing.
     * Verifies the object actually exists in R2 before marking the row
     * UPLOADED and enqueuing it for async processing.
     */
    JobCardMedia completeUpload(Long mediaId, com.garageos.modules.media.dto.request.UploadCompleteRequest request);

    /**
     * Returns how the client should fetch this media's bytes — a direct R2
     * presigned URL, or (for legacy GOOGLE_DRIVE rows) the existing proxied
     * content endpoint. Authorizes the same way as {@link #getMediaContent}.
     *
     * @param variant "original" (default) or "thumbnail". Requesting
     *                "thumbnail" for a video with no generated thumbnail
     *                (always true today — see MediaProcessingScheduler)
     *                returns {@code available=false} rather than ever
     *                falling back to the full video, so a gallery grid never
     *                accidentally downloads an entire video just to show a
     *                tile. Requesting "thumbnail" for an image with no
     *                thumbnail yet (still PROCESSING, or a legacy Drive row)
     *                falls back to the original — acceptable since a photo
     *                is small.
     */
    MediaAccessResponse getPlaybackAccess(Long mediaId, String variant);

    /**
     * The provider/variant resolution half of {@link #getPlaybackAccess} —
     * same R2-presigned-vs-Drive-proxy branching, same thumbnail/evidence/
     * original fallback rules — but takes an already-authorized
     * {@link JobCardMedia} and performs no authorization of its own.
     * Authorization and provider resolution are deliberately separate calls:
     * {@link #getPlaybackAccess} does employee authorization before
     * delegating here; a customer-facing caller (CustomerPortalServiceImpl)
     * does its own customer/job-card-ownership authorization first, then
     * calls this directly, so the two access paths never share (or bypass)
     * each other's authorization rules.
     */
    MediaAccessResponse resolvePlaybackAccess(JobCardMedia media, String variant);

    /**
     * Deletes a media row and best-effort deletes its underlying object
     * (R2) or logs the known Drive limitation (no Drive delete capability —
     * see GoogleDriveMediaStorageProvider). Restricted to privileged roles
     * at the controller level, same as {@link #updateVisibility}.
     */
    void deleteMedia(Long mediaId);
}