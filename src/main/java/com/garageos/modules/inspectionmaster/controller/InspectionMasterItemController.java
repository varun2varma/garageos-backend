package com.garageos.modules.inspectionmaster.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.inspectionmaster.dto.request.CreateInspectionMasterItemRequest;
import com.garageos.modules.inspectionmaster.dto.response.InspectionMasterItemResponse;
import com.garageos.modules.inspectionmaster.service.InspectionMasterItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inspection-master")
@RequiredArgsConstructor
public class InspectionMasterItemController {

    private final InspectionMasterItemService service;

    @PostMapping("/{masterId}/items")
    public ResponseEntity<ApiResponse<InspectionMasterItemResponse>> createItem(
            @PathVariable Long masterId,
            @Valid @RequestBody CreateInspectionMasterItemRequest request) {

        return ApiResponseUtil.created(
                "Inspection Master Item created successfully.",
                service.createInspectionMasterItem(masterId, request));
    }

    @GetMapping("/{masterId}/items")
    public ResponseEntity<ApiResponse<List<InspectionMasterItemResponse>>> getItems(
            @PathVariable Long masterId) {

        return ApiResponseUtil.success(
                "Inspection Master Items fetched successfully.",
                service.getInspectionMasterItems(masterId));
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<ApiResponse<InspectionMasterItemResponse>> getItem(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Inspection Master Item fetched successfully.",
                service.getInspectionMasterItem(id));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<ApiResponse<InspectionMasterItemResponse>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody CreateInspectionMasterItemRequest request) {

        return ApiResponseUtil.success(
                "Inspection Master Item updated successfully.",
                service.updateInspectionMasterItem(id, request));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Long id) {

        service.deleteInspectionMasterItem(id);

        return ApiResponseUtil.success(
                "Inspection Master Item deleted successfully.");
    }
}