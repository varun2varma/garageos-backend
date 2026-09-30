package com.garageos.modules.media.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response for {@code POST /job-cards/{jobCardId}/media/upload-intent}.
 * Deliberately carries only what Flutter needs to perform the direct upload
 * and later confirm it — never an R2 access key/secret. {@code uploadUrl} is
 * itself a bearer credential for this one object/TTL and should not be
 * logged by the client either.
 */
@Getter
@Setter
@Builder
public class UploadIntentResponse {

    private Long mediaId;

    private String uploadUrl;

    private String method;

    private Map<String, String> requiredHeaders;

    private String storageProvider;

    private String storageKey;

    private LocalDateTime expiresAt;

    /** Client must echo this back on {@code /media/{id}/complete} — the idempotency key for that call. */
    private String uploadSessionId;

    private String status;

    /**
     * VIDEO only: a second presigned PUT for the thumbnail JPEG (the client
     * extracts it from the final burned-in evidence video). Null for images,
     * whose thumbnail the backend generates itself.
     */
    private String thumbnailUploadUrl;

    private Map<String, String> thumbnailRequiredHeaders;

    /**
     * The exact evidence text (stage, capture time, captured-by, address,
     * coordinates, accuracy) built from the persisted capture metadata. The
     * client burns these lines into a VIDEO; images are rendered server-side
     * from the same source (MediaEvidenceText).
     */
    private java.util.List<String> evidenceLines;
}
