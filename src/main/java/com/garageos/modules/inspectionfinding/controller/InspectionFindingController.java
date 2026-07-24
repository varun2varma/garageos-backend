package com.garageos.modules.inspectionfinding.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.inspectionfinding.dto.request.CreateInspectionFindingRequest;
import com.garageos.modules.inspectionfinding.dto.response.InspectionFindingResponse;
import com.garageos.modules.inspectionfinding.service.InspectionFindingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inspection-findings")
@RequiredArgsConstructor
public class InspectionFindingController {

    private final InspectionFindingService service;

    @PostMapping("/jobcards/{jobCardId}")
    public ResponseEntity<ApiResponse<InspectionFindingResponse>> createInspectionFinding(
            @PathVariable Long jobCardId,
            @Valid @RequestBody CreateInspectionFindingRequest request) {

        return ApiResponseUtil.created(
                "Inspection Finding created successfully.",
                service.createInspectionFinding(jobCardId, request));
    }

    @PostMapping("/jobcards/{jobCardId}/load")
    public ResponseEntity<ApiResponse<Void>> loadInspectionTemplate(
            @PathVariable Long jobCardId) {

        service.loadInspectionTemplate(jobCardId);

        return ApiResponseUtil.success(
                "Inspection checklist loaded successfully.");
    }

    @GetMapping("/jobcards/{jobCardId}")
    public ResponseEntity<ApiResponse<List<InspectionFindingResponse>>> getInspectionFindings(
            @PathVariable Long jobCardId) {

        return ApiResponseUtil.success(
                "Inspection Findings fetched successfully.",
                service.getInspectionFindings(jobCardId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InspectionFindingResponse>> getInspectionFinding(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Inspection Finding fetched successfully.",
                service.getInspectionFinding(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InspectionFindingResponse>> updateInspectionFinding(
            @PathVariable Long id,
            @Valid @RequestBody CreateInspectionFindingRequest request) {

        return ApiResponseUtil.success(
                "Inspection Finding updated successfully.",
                service.updateInspectionFinding(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInspectionFinding(
            @PathVariable Long id) {

        service.deleteInspectionFinding(id);

        return ApiResponseUtil.success(
                "Inspection Finding deleted successfully.");
    }

}