package com.garageos.modules.serviceworkflow.service.impl;

import com.garageos.modules.jobcard.dto.request.CreateJobCardRequest;
import com.garageos.modules.jobcard.dto.response.JobCardResponse;
import com.garageos.modules.jobcard.service.JobCardService;
import com.garageos.modules.serviceworkflow.dto.response.WorkflowResponse;
import com.garageos.modules.serviceworkflow.service.ServiceWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ServiceWorkflowServiceImpl
        implements ServiceWorkflowService {

    private final JobCardService jobCardService;

    @Override
    public WorkflowResponse createJob(CreateJobCardRequest request) {

        JobCardResponse response =
                jobCardService.createJobCard(request);

        return WorkflowResponse.builder()
                .data(response)
                .message("Job created successfully.")
                .build();
    }

    @Override
    public WorkflowResponse startInspection(String jobCardNumber) {

        return WorkflowResponse.builder()
                .data(jobCardService.startInspection(jobCardNumber))
                .message("Inspection started successfully.")
                .build();
    }

    @Override
    public WorkflowResponse completeInspection(String jobCardNumber) {

        return WorkflowResponse.builder()
                .data(jobCardService.completeInspection(jobCardNumber))
                .message("Inspection completed successfully.")
                .build();
    }

    @Override
    public WorkflowResponse prepareEstimate(String jobCardNumber) {

        return WorkflowResponse.builder()
                .data(jobCardService.prepareEstimate(jobCardNumber))
                .message("Estimate prepared successfully.")
                .build();
    }

    @Override
    public WorkflowResponse approveEstimate(String jobCardNumber) {

        return WorkflowResponse.builder()
                .data(jobCardService.approveEstimate(jobCardNumber))
                .message("Estimate approved successfully.")
                .build();
    }

    @Override
    public WorkflowResponse startRepair(String jobCardNumber) {

        return WorkflowResponse.builder()
                .data(jobCardService.startRepair(jobCardNumber))
                .message("Repair started successfully.")
                .build();
    }

    @Override
    public WorkflowResponse completeRepair(String jobCardNumber) {

        return WorkflowResponse.builder()
                .data(jobCardService.completeRepair(jobCardNumber))
                .message("Repair completed successfully.")
                .build();
    }

    @Override
    public WorkflowResponse performQualityCheck(String jobCardNumber) {

        return WorkflowResponse.builder()
                .data(jobCardService.performQualityCheck(jobCardNumber))
                .message("Quality check completed successfully.")
                .build();
    }

    @Override
    public WorkflowResponse readyForDelivery(String jobCardNumber) {

        return WorkflowResponse.builder()
                .data(jobCardService.readyForDelivery(jobCardNumber))
                .message("Vehicle is ready for delivery.")
                .build();
    }

    @Override
    public WorkflowResponse closeJob(String jobCardNumber) {

        return WorkflowResponse.builder()
                .data(jobCardService.closeJobCard(jobCardNumber))
                .message("Job closed successfully.")
                .build();
    }
}