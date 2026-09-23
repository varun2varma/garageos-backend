package com.garageos.modules.estimateitem.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.estimateitem.dto.request.CreateEstimateItemRequest;
import com.garageos.modules.estimateitem.dto.request.SetSelectionRequest;
import com.garageos.modules.estimateitem.dto.response.EstimateItemResponse;
import com.garageos.modules.estimateitem.service.EstimateItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EstimateItemController {

    private final EstimateItemService service;

    @PostMapping("/estimates/{estimateId}/items")
    public ResponseEntity<ApiResponse<EstimateItemResponse>> addItem(
            @PathVariable Long estimateId,
            @Valid @RequestBody CreateEstimateItemRequest request) {

        return ApiResponseUtil.created(
                "Estimate item added successfully.",
                service.addItem(estimateId, request)
        );
    }

    @GetMapping("/estimates/{estimateId}/items")
    public ResponseEntity<ApiResponse<List<EstimateItemResponse>>> getItems(
            @PathVariable Long estimateId) {

        return ApiResponseUtil.success(
                "Estimate items fetched successfully.",
                service.getItems(estimateId)
        );
    }

    @GetMapping("/estimate-items/{id}")
    public ResponseEntity<ApiResponse<EstimateItemResponse>> getItem(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Estimate item fetched successfully.",
                service.getItem(id)
        );
    }

    /**
     * Additive read, scoped to one Complaint (and therefore, since
     * RepairTask<->Complaint is 1:1, to one RepairTask's worth of
     * PART/LABOUR line items) — for the technician Repair Task Details
     * screen. Returns the existing EstimateItemResponse; no new DTO.
     */
    @GetMapping("/estimate-items")
    public ResponseEntity<ApiResponse<List<EstimateItemResponse>>> getItemsByComplaint(
            @RequestParam Long complaintId) {

        return ApiResponseUtil.success(
                "Estimate items fetched successfully.",
                service.getItemsByComplaint(complaintId)
        );
    }

    @PutMapping("/estimate-items/{id}")
    public ResponseEntity<ApiResponse<EstimateItemResponse>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody CreateEstimateItemRequest request) {

        return ApiResponseUtil.success(
                "Estimate item updated successfully.",
                service.updateItem(id, request)
        );
    }

    /**
     * Mission: customer select/deselect of an individual estimate item,
     * before approval — role/ownership/approval-state checked in
     * EstimateItemServiceImpl.setItemSelection.
     */
    @PutMapping("/estimate-items/{id}/selection")
    public ResponseEntity<ApiResponse<EstimateItemResponse>> setSelection(
            @PathVariable Long id,
            @Valid @RequestBody SetSelectionRequest request) {

        return ApiResponseUtil.success(
                "Selection updated successfully.",
                service.setItemSelection(id, request.getSelected())
        );
    }

    @DeleteMapping("/estimate-items/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Long id) {

        service.deleteItem(id);

        return ApiResponseUtil.success(
                "Estimate item deleted successfully."
        );
    }
}