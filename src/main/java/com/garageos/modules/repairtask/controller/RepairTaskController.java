package com.garageos.modules.repairtask.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.repairtask.dto.request.AssignTechnicianRequest;
import com.garageos.modules.repairtask.dto.response.RepairTaskResponse;
import com.garageos.modules.repairtask.service.RepairTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/repair-tasks")
@RequiredArgsConstructor
public class RepairTaskController {

    private final RepairTaskService service;

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
    public ResponseEntity<ApiResponse<RepairTaskResponse>> assignTechnician(
            @PathVariable Long id,
            @Valid @RequestBody AssignTechnicianRequest request) {

        return ApiResponseUtil.success(
                "Technician assigned successfully.",
                service.assignTechnician(
                        id,
                        request.getTechnicianName()
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

}