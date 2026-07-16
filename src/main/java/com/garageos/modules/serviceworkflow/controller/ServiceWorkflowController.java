package com.garageos.modules.serviceworkflow.controller;

import com.garageos.modules.jobcard.dto.request.CreateJobCardRequest;
import com.garageos.modules.serviceworkflow.dto.response.WorkflowResponse;
import com.garageos.modules.serviceworkflow.service.ServiceWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workflow")
@RequiredArgsConstructor
public class ServiceWorkflowController {

    private final ServiceWorkflowService workflowService;

    @PostMapping("/job")
    public ResponseEntity<WorkflowResponse> createJob(
            @Valid @RequestBody CreateJobCardRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workflowService.createJob(request));
    }

    @PostMapping("/{jobCardNumber}/inspection/start")
    public ResponseEntity<WorkflowResponse> startInspection(
            @PathVariable String jobCardNumber) {

        return ResponseEntity.ok(
                workflowService.startInspection(jobCardNumber));
    }

    @PostMapping("/{jobCardNumber}/inspection/complete")
    public ResponseEntity<WorkflowResponse> completeInspection(
            @PathVariable String jobCardNumber) {

        return ResponseEntity.ok(
                workflowService.completeInspection(jobCardNumber));
    }

    @PostMapping("/{jobCardNumber}/estimate/prepare")
    public ResponseEntity<WorkflowResponse> prepareEstimate(
            @PathVariable String jobCardNumber) {

        return ResponseEntity.ok(
                workflowService.prepareEstimate(jobCardNumber));
    }

    @PostMapping("/{jobCardNumber}/estimate/approve")
    public ResponseEntity<WorkflowResponse> approveEstimate(
            @PathVariable String jobCardNumber) {

        return ResponseEntity.ok(
                workflowService.approveEstimate(jobCardNumber));
    }

    @PostMapping("/{jobCardNumber}/repair/start")
    public ResponseEntity<WorkflowResponse> startRepair(
            @PathVariable String jobCardNumber) {

        return ResponseEntity.ok(
                workflowService.startRepair(jobCardNumber));
    }

    @PostMapping("/{jobCardNumber}/repair/complete")
    public ResponseEntity<WorkflowResponse> completeRepair(
            @PathVariable String jobCardNumber) {

        return ResponseEntity.ok(
                workflowService.completeRepair(jobCardNumber));
    }

    @PostMapping("/{jobCardNumber}/quality-check")
    public ResponseEntity<WorkflowResponse> qualityCheck(
            @PathVariable String jobCardNumber) {

        return ResponseEntity.ok(
                workflowService.performQualityCheck(jobCardNumber));
    }

    @PostMapping("/{jobCardNumber}/delivery")
    public ResponseEntity<WorkflowResponse> readyForDelivery(
            @PathVariable String jobCardNumber) {

        return ResponseEntity.ok(
                workflowService.readyForDelivery(jobCardNumber));
    }

    @PostMapping("/{jobCardNumber}/close")
    public ResponseEntity<WorkflowResponse> closeJob(
            @PathVariable String jobCardNumber) {

        return ResponseEntity.ok(
                workflowService.closeJob(jobCardNumber));
    }
}