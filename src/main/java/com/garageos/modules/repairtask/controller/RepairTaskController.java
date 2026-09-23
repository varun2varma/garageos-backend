package com.garageos.modules.repairtask.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.repairtask.dto.request.AssignTechnicianRequest;
import com.garageos.modules.repairtask.dto.request.SetPriorityRequest;
import com.garageos.modules.repairtask.dto.response.RepairTaskResponse;
import com.garageos.modules.repairtask.service.RepairTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/repair-tasks")
@RequiredArgsConstructor
public class RepairTaskController {

    private final RepairTaskService service;

    /**
     * Locked operational-access decision: assigning a technician
     * (including via this legacy free-text endpoint) is an
     * OWNER/MANAGER/SERVICE_ADVISOR action, matching
     * JobAssignmentController's ASSIGNMENT_ROLES.
     */
    private static final String ASSIGNMENT_ROLES = """
            hasAnyRole(
                'MANAGER',
                'SERVICE_ADVISOR',
                'OWNER'
            )
            """;

    @GetMapping("/jobcards/{jobCardId}")
    public ResponseEntity<ApiResponse<List<RepairTaskResponse>>> getRepairTasks(
            @PathVariable Long jobCardId) {

        return ApiResponseUtil.success(
                "Repair tasks fetched successfully.",
                service.getRepairTasks(jobCardId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RepairTaskResponse>> getRepairTask(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Repair task fetched successfully.",
                service.getRepairTask(id)
        );
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize(ASSIGNMENT_ROLES)
    public ResponseEntity<ApiResponse<RepairTaskResponse>> assignTechnician(
            @PathVariable Long id,
            @Valid @RequestBody AssignTechnicianRequest request) {

        return ApiResponseUtil.success(
                "Technician assigned successfully.",
                service.assignTechnician(
                        id,
                        request
                )
        );
    }

    @PutMapping("/{id}/start")
    public ResponseEntity<ApiResponse<RepairTaskResponse>> startRepair(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Repair started successfully.",
                service.startRepair(id)
        );
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<RepairTaskResponse>> completeRepair(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Repair completed successfully.",
                service.completeRepair(id)
        );
    }

    /**
     * Mission backlog #20 — Service Advisor/Manager/Owner only. A
     * technician cannot set the priority of their own work.
     */
    @PutMapping("/{id}/priority")
    @PreAuthorize(ASSIGNMENT_ROLES)
    public ResponseEntity<ApiResponse<RepairTaskResponse>> setPriority(
            @PathVariable Long id,
            @Valid @RequestBody SetPriorityRequest request) {

        return ApiResponseUtil.success(
                "Priority updated successfully.",
                service.setPriority(id, request.getPriority())
        );
    }

}