package com.garageos.modules.media.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for {@code POST /media/{mediaId}/complete}. {@code uploadSessionId}
 * is the idempotency key — a retried call with the same value returns the
 * already-completed row rather than reprocessing (see
 * MediaServiceImpl.completeUpload).
 */
@Getter
@Setter
public class UploadCompleteRequest {

    @NotBlank
    private String uploadSessionId;

    /** Optional — compared against what R2 actually reports, not trusted alone. */
    private Long fileSize;

    private String checksum;

    /**
     * Video duration, extracted client-side (see Flutter's
     * MediaUploadQueueService, which briefly initializes a VideoPlayerController
     * on the local file before upload — reads the header/metadata via the
     * platform's own decoder, does not buffer the whole file into Dart
     * memory). Null for images and for a video where extraction failed
     * (never blocks the upload).
     */
    private Integer durationSeconds;

    /**
     * VIDEO only: the client has uploaded the thumbnail JPEG to the
     * thumbnail URL returned by the upload intent. The backend verifies the
     * object exists before recording it.
     */
    private Boolean thumbnailUploaded;
}
