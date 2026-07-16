package com.garageos.modules.complaint.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.complaint.dto.request.CreateComplaintRequest;
import com.garageos.modules.complaint.dto.response.ComplaintResponse;
import com.garageos.modules.complaint.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService service;

    @PostMapping("/jobcards/{jobCardId}/complaints")
    public ResponseEntity<ApiResponse<List<ComplaintResponse>>> createComplaint(
            @PathVariable Long jobCardId,
            @Valid @RequestBody List<CreateComplaintRequest> request) {

        return ApiResponseUtil.created(
                "Complaint created successfully.",
                service.createComplaintList(jobCardId, request));
    }

    @GetMapping("/jobcards/{jobCardId}/complaints")
    public ResponseEntity<ApiResponse<List<ComplaintResponse>>> getComplaints(
            @PathVariable Long jobCardId) {

        return ApiResponseUtil.success(
                "Complaints fetched successfully.",
                service.getComplaints(jobCardId));
    }

    @GetMapping("/complaints/{id}")
    public ResponseEntity<ApiResponse<ComplaintResponse>> getComplaint(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Complaint fetched successfully.",
                service.getComplaint(id));
    }

    @PutMapping("/complaints/{id}")
    public ResponseEntity<ApiResponse<ComplaintResponse>> updateComplaint(
            @PathVariable Long id,
            @Valid @RequestBody CreateComplaintRequest request) {

        return ApiResponseUtil.success(
                "Complaint updated successfully.",
                service.updateComplaint(id, request));
    }

    @DeleteMapping("/complaints/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComplaint(
            @PathVariable Long id) {

        service.deleteComplaint(id);

        return ApiResponseUtil.success(
                "Complaint deleted successfully.");
    }
}