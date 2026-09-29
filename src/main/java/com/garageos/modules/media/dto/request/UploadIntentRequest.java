package com.garageos.modules.media.dto.request;

import com.garageos.core.enums.media.MediaStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for {@code POST /job-cards/{jobCardId}/media/upload-intent} —
 * the first step of the direct-to-R2 upload flow. Mirrors the same
 * fields the legacy multipart endpoint accepts as request params
 * (stage/repairTaskId), plus what the backend needs to know BEFORE bytes
 * exist anywhere: content type and declared size (both re-validated,
 * never trusted blindly — see MediaServiceImpl.createUploadIntent).
 */
@Getter
@Setter
public class UploadIntentRequest {

    @NotNull
    private MediaStage stage;

    private Long repairTaskId;

    @NotBlank
    private String contentType;

    @NotNull
    @Positive
    private Long fileSize;

    /** Optional client-computed checksum, recorded for later integrity comparison at completion time. */
    private String checksum;

    /**
     * ISO-8601 device-clock timestamp of the actual capture/selection
     * moment (NOT the upload moment — the device may have been offline for
     * some time in between). Trusted for audit context, not an identity
     * claim. Falls back to server-received time in MediaServiceImpl if
     * absent — see that class's own doc comment; never a fabricated
     * historical value.
     */
    private String capturedAt;

    /** Client-reported GPS fix at capture time. All three are null together if location was unavailable/denied. */
    private Double latitude;

    private Double longitude;

    private Double locationAccuracyMeters;

    /** Best-effort reverse-geocoded snapshot; may be null even when coordinates are present. */
    private String locationName;
}
