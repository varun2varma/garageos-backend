package com.garageos.modules.jobassignment.dto.request;

import com.garageos.core.enums.JobAssignmentType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignJobRequest {

    @NotNull
    private Long jobCardId;

    private Long estimateItemId;

    @NotNull
    private Long employeeId;

    @NotNull
    private JobAssignmentType assignmentType;

    private Double estimatedHours;

    private String remarks;
}