package com.garageos.modules.jobassignment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignJobRequest {

    @NotNull
    private Long jobCardId;

    @NotNull
    private Long estimateItemId;

    @NotNull
    private Long employeeId;

    private Double estimatedHours;

    private String remarks;

}