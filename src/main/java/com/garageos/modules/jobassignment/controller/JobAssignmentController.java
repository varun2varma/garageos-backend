package com.garageos.modules.jobassignment.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.jobassignment.dto.request.AssignJobRequest;
import com.garageos.modules.jobassignment.dto.request.CompleteJobRequest;
import com.garageos.modules.jobassignment.dto.request.ReassignJobRequest;
import com.garageos.modules.jobassignment.dto.request.StartJobRequest;
import com.garageos.modules.jobassignment.dto.response.JobAssignmentResponse;
import com.garageos.modules.jobassignment.dto.response.MyAssignmentResponse;
import com.garageos.modules.jobassignment.service.JobAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/job-assignments")
@RequiredArgsConstructor
public class JobAssignmentController {

    private final JobAssignmentService service;

    @PostMapping
    public ResponseEntity<ApiResponse<JobAssignmentResponse>> assignJob(
            @Valid @RequestBody AssignJobRequest request) {

        return ApiResponseUtil.created(
                "Job assigned successfully.",
                service.assignJob(request)
        );

    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<JobAssignmentResponse>> acceptJob(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Job accepted successfully.",
                service.acceptJob(id)
        );

    }

    @PutMapping("/{id}/start")
    public ResponseEntity<ApiResponse<JobAssignmentResponse>> startJob(
            @PathVariable Long id,
            @Valid @RequestBody StartJobRequest request) {

        return ApiResponseUtil.success(
                "Job started successfully.",
                service.startJob(id, request)
        );

    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<JobAssignmentResponse>> completeJob(
            @PathVariable Long id,
            @Valid @RequestBody CompleteJobRequest request) {

        return ApiResponseUtil.success(
                "Job completed successfully.",
                service.completeJob(id, request)
        );

    }

    @PutMapping("/{id}/reassign")
    public ResponseEntity<ApiResponse<JobAssignmentResponse>> reassignJob(
            @PathVariable Long id,
            @Valid @RequestBody ReassignJobRequest request) {

        return ApiResponseUtil.success(
                "Job reassigned successfully.",
                service.reassignJob(id, request)
        );

    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobAssignmentResponse>> getAssignment(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Job assignment fetched successfully.",
                service.getAssignment(id)
        );

    }

    @GetMapping("/job-card/{jobCardId}")
    public ResponseEntity<ApiResponse<List<JobAssignmentResponse>>> getAssignmentsByJobCard(
            @PathVariable Long jobCardId) {

        return ApiResponseUtil.success(
                "Job assignments fetched successfully.",
                service.getAssignmentsByJobCard(jobCardId)
        );

    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<MyAssignmentResponse>>> getMyAssignments(
            @AuthenticationPrincipal GarageUserPrincipal user) {

        return ApiResponseUtil.success(
                "My assignments fetched successfully.",
                service.getMyAssignments(user.getId())
        );

    }

}