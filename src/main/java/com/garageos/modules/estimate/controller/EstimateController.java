package com.garageos.modules.estimate.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.estimate.dto.request.CreateEstimateRequest;
import com.garageos.modules.estimate.dto.response.EstimateResponse;
import com.garageos.modules.estimate.service.EstimateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/estimates")
@RequiredArgsConstructor
public class EstimateController {

    private final EstimateService service;

    @PostMapping
    public ResponseEntity<ApiResponse<EstimateResponse>> createEstimate(
            @Valid @RequestBody CreateEstimateRequest request) {

        return ApiResponseUtil.created(
                "Estimate created successfully.",
                service.createEstimate(request)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EstimateResponse>> getEstimate(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Estimate fetched successfully.",
                service.getEstimate(id)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EstimateResponse>>> getAllEstimates() {

        return ApiResponseUtil.success(
                "Estimates fetched successfully.",
                service.getAllEstimates()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EstimateResponse>> updateEstimate(
            @PathVariable Long id,
            @Valid @RequestBody CreateEstimateRequest request) {

        return ApiResponseUtil.success(
                "Estimate updated successfully.",
                service.updateEstimate(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEstimate(
            @PathVariable Long id) {

        service.deleteEstimate(id);

        return ApiResponseUtil.success(
                "Estimate deleted successfully."
        );
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<EstimateResponse>> approveEstimate(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Estimate approved successfully.",
                service.approveEstimate(id)
        );
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<EstimateResponse>> rejectEstimate(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Estimate rejected successfully.",
                service.rejectEstimate(id)
        );
    }
}