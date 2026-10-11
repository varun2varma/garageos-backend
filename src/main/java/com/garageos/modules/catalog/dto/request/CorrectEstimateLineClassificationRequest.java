package com.garageos.modules.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Corrects the classification snapshot of one line of an APPROVED estimate that has no invoice yet.
 * The classification always comes from an existing, active catalog item of the matching type; the caller never
 * types a code or a rate.
 */
@Getter
@Setter
public class CorrectEstimateLineClassificationRequest {

    @NotNull(message = "The catalog item is required.")
    private Long catalogItemId;

    /** Why the correction is needed; stored in the audit trail. Do not include customer personal data. */
    @NotBlank(message = "A correction reason is required.")
    @Size(min = 10, max = 500, message = "The reason must be 10 to 500 characters.")
    private String reason;
}
