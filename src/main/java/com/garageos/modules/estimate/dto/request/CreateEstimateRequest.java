package com.garageos.modules.estimate.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateEstimateRequest {

    @NotNull(message = "Job Card Id is required.")
    private Long jobCardId;

    private String remarks;

}