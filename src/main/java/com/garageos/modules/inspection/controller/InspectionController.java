package com.garageos.modules.inspection.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.inspection.dto.request.CreateInspectionRequest;
import com.garageos.modules.inspection.dto.response.InspectionResponse;
import com.garageos.modules.inspection.service.InspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService service;

    @PostMapping("/complaints/{complaintId}/inspections")
    public ResponseEntity<ApiResponse<InspectionResponse>> createInspection(
            @PathVariable Long complaintId,
            @Valid @RequestBody CreateInspectionRequest request) {

        return ApiResponseUtil.created(
                "Inspection created successfully.",
                service.createInspection(complaintId, request));
    }

    @GetMapping("/complaints/{complaintId}/inspections")
    public ResponseEntity<ApiResponse<List<InspectionResponse>>> getInspections(
            @PathVariable Long complaintId) {

        return ApiResponseUtil.success(
                "Inspections fetched successfully.",
                service.getInspections(complaintId));
    }

    @GetMapping("/inspections/{id}")
    public ResponseEntity<ApiResponse<InspectionResponse>> getInspection(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Inspection fetched successfully.",
                service.getInspection(id));
    }

    @PutMapping("/inspections/{id}")
    public ResponseEntity<ApiResponse<InspectionResponse>> updateInspection(
            @PathVariable Long id,
            @Valid @RequestBody CreateInspectionRequest request) {

        return ApiResponseUtil.success(
                "Inspection updated successfully.",
                service.updateInspection(id, request));
    }

    @DeleteMapping("/inspections/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInspection(
            @PathVariable Long id) {

        service.deleteInspection(id);

        return ApiResponseUtil.success(
                "Inspection deleted successfully.");
    }
}