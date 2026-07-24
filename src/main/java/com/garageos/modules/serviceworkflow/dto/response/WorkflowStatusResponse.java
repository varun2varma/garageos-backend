package com.garageos.modules.serviceworkflow.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WorkflowStatusResponse {

    private String jobCardNumber;

    private String status;

    private int nextStep;

    private int progress;

    private List<String> completedSteps;

}