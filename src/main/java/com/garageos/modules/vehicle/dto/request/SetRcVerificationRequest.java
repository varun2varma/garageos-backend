package com.garageos.modules.vehicle.dto.request;

import com.garageos.core.enums.vehicle.RcVerificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetRcVerificationRequest {

    @NotNull(message = "Status is required.")
    private RcVerificationStatus status;

    /** Optional reference/key to where the RC document lives — never the document itself. */
    private String documentReference;
}
