package com.garageos.modules.serviceworkflow.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.inspection.dto.request.CreateInspectionRequest;
import com.garageos.modules.jobcard.dto.request.CreateJobCardRequest;
import com.garageos.modules.repairtask.dto.response.RepairTaskResponse;
import com.garageos.modules.serviceworkflow.dto.response.WorkflowResponse;
import com.garageos.modules.serviceworkflow.dto.response.WorkflowStatusResponse;
import com.garageos.modules.serviceworkflow.service.ServiceWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<ApiResponse<WorkflowResponse>> startInspection(
            @PathVariable String jobCardNumber) {

        return ApiResponseUtil.success(
                "Inspection started successfully.",
                workflowService.startInspection(jobCardNumber));
    }

    @PostMapping("/{jobCardNumber}/inspection/complete")
    public ResponseEntity<ApiResponse<WorkflowResponse>> completeInspection(
            @PathVariable String jobCardNumber,
            @RequestBody List<CreateInspectionRequest> request) {

        return ApiResponseUtil.success(
                "Inspection completed successfully.",
                workflowService.completeInspection(
                        jobCardNumber,
                        request));
    }

    @PostMapping("/{jobCardNumber}/estimate")
    public ResponseEntity<ApiResponse<WorkflowResponse>>
    prepareEstimate(
            @PathVariable String jobCardNumber) {

        return ApiResponseUtil.success(
                "Estimate prepared successfully.",
                workflowService.prepareEstimate(jobCardNumber));
    }

    @PostMapping("/{jobCardNumber}/estimate/approve")
    public ResponseEntity<ApiResponse<WorkflowResponse>> approveEstimate(
            @PathVariable String jobCardNumber) {

        return ApiResponseUtil.success(
                "Estimate approved successfully.",
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

    @PostMapping("/{jobCardNumber}/invoice")
    public ResponseEntity<ApiResponse<WorkflowResponse>>
    generateInvoice(
            @PathVariable String jobCardNumber) {

        return ApiResponseUtil.success(
                "Invoice generated successfully.",
                workflowService.generateInvoice(jobCardNumber));
    }
    @PostMapping("/{jobCardNumber}/payment")
    public ResponseEntity<ApiResponse<WorkflowResponse>>
    receivePayment(
            @PathVariable String jobCardNumber) {

        return ApiResponseUtil.success(
                "Payment received successfully.",
                workflowService.receivePayment(jobCardNumber));
    }

    @GetMapping("/{jobCardNumber}/repair-tasks")
    public ResponseEntity<ApiResponse<List<RepairTaskResponse>>> getRepairTasks(
            @PathVariable String jobCardNumber) {

        return ApiResponseUtil.success(
                "Repair tasks fetched successfully.",
                workflowService.getRepairTasks(jobCardNumber));
    }

    @GetMapping("/{jobCardNumber}/status")
    public ResponseEntity<ApiResponse<WorkflowStatusResponse>>
    getWorkflowStatus(
            @PathVariable String jobCardNumber){

        return ApiResponseUtil.success(
                "Workflow status fetched successfully.",
                workflowService.getWorkflowStatus(jobCardNumber));

    }
}