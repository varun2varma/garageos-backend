package com.garageos.modules.serviceworkflow.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.inspection.dto.request.CreateInspectionRequest;
import com.garageos.modules.invoice.dto.response.InvoiceResponse;
import com.garageos.modules.jobcard.dto.request.CreateJobCardRequest;
import com.garageos.modules.repairtask.dto.response.RepairTaskResponse;
import com.garageos.modules.serviceworkflow.dto.response.WorkflowResponse;
import com.garageos.modules.serviceworkflow.dto.response.WorkflowResumeResponse;
import com.garageos.modules.serviceworkflow.dto.response.WorkflowStatusResponse;
import com.garageos.modules.serviceworkflow.service.ServiceWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workflow")
@RequiredArgsConstructor
public class ServiceWorkflowController {

    private final ServiceWorkflowService workflowService;

    /**
     * Locked operational-access decision: OWNER/MANAGER/SERVICE_ADVISOR
     * for general operational JobCard actions on this controller.
     * estimate/approve is deliberately NOT annotated here — it is
     * enforced at the service layer (EstimateServiceImpl
     * .authorizeEmployeeEstimateApproval, MANAGER-only) so the business
     * operation cannot be bypassed by another controller/entry point;
     * duplicating a different role set here would risk the two checks
     * drifting out of sync. repair/start is the same: it is the
     * manager-only "Proceed to Repair" confirmation gate between
     * customer estimate approval and REPAIR_IN_PROGRESS, enforced
     * MANAGER-only at the service layer
     * (JobCardServiceImpl.authorizeProceedToRepair) for the identical
     * reason — it is reachable from this controller and from
     * JobCardController.
     */
    private static final String WORKFLOW_OPERATIONAL_ROLES = """
            hasAnyRole(
                'MANAGER',
                'SERVICE_ADVISOR',
                'OWNER'
            )
            """;

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
    @PreAuthorize(WORKFLOW_OPERATIONAL_ROLES)
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
    @PreAuthorize(WORKFLOW_OPERATIONAL_ROLES)
    public ResponseEntity<WorkflowResponse> closeJob(
            @PathVariable String jobCardNumber) {

        return ResponseEntity.ok(
                workflowService.closeJob(jobCardNumber));
    }

    @PostMapping("/{jobCardNumber}/invoice")
    @PreAuthorize(WORKFLOW_OPERATIONAL_ROLES)
    public ResponseEntity<ApiResponse<InvoiceResponse>>
    generateInvoice(
            @PathVariable String jobCardNumber) {

        return ApiResponseUtil.success(
                "Invoice generated successfully.",
                workflowService.generateInvoice(jobCardNumber));
    }
    /**
     * New customer-only acceptance gate, inserted between invoice
     * generation and payment: Invoice Generated -> Customer accepts ->
     * Payment becomes available. CUSTOMER-only (not
     * WORKFLOW_OPERATIONAL_ROLES) because this is a customer self-service
     * step; staff never need to "accept" an invoice on a customer's
     * behalf. Ownership (this job card is actually the caller's) is
     * enforced in InvoiceServiceImpl.acceptInvoice the same way it is for
     * receivePayment below. Staff-recorded in-person payments
     * (MANAGER/SERVICE_ADVISOR/OWNER via receivePayment) do not go through
     * this endpoint at all, so the existing counter/cash flow is
     * unaffected; InvoiceServiceImpl.receivePayment only requires
     * ACCEPTED when the caller is a CUSTOMER.
     */
    @PostMapping("/{jobCardNumber}/invoice/accept")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> acceptInvoice(
            @PathVariable String jobCardNumber) {

        return ApiResponseUtil.success(
                "Invoice accepted successfully.",
                workflowService.acceptInvoice(jobCardNumber));
    }

    /**
     * Deliberately NOT WORKFLOW_OPERATIONAL_ROLES: unlike every other
     * action on this controller, paying an invoice is a customer
     * self-service action, not a staff operation - the legacy static
     * customer portal (customer/js/invoice.js -> customerPortalService.js)
     * calls this exact endpoint directly. CUSTOMER is added here only,
     * not to the shared constant, so no other action on this controller
     * is affected. Ownership for a CUSTOMER caller (this job card is
     * actually theirs) is enforced in InvoiceServiceImpl.receivePayment,
     * since @PreAuthorize only checks role, not which job card. For a
     * CUSTOMER caller specifically, InvoiceServiceImpl.receivePayment also
     * now requires the invoice to already be InvoiceStatus.ACCEPTED (see
     * acceptInvoice above) - staff callers are exempt from that gate so
     * an in-person/cash payment recorded by staff is not blocked on the
     * customer ever having opened the app.
     */
    @PostMapping("/{jobCardNumber}/payment")
    @PreAuthorize("hasAnyRole('MANAGER', 'SERVICE_ADVISOR', 'OWNER', 'CUSTOMER')")
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

    @GetMapping("/{jobCardNumber}/resume")
    public ResponseEntity<ApiResponse<WorkflowResumeResponse>>
    resumeWorkflow(
            @PathVariable String jobCardNumber) {

        return ApiResponseUtil.success(
                "Workflow resumed successfully.",
                workflowService.resumeWorkflow(jobCardNumber)
        );

    }
}