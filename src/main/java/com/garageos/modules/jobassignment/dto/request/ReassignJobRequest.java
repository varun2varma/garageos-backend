package com.garageos.modules.jobassignment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReassignJobRequest {

    @NotNull
    private Long employeeId;

    private String remarks;

}