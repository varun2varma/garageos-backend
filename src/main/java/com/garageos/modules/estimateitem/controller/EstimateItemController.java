package com.garageos.modules.estimateitem.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.estimateitem.dto.request.CreateEstimateItemRequest;
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

    @PutMapping("/estimate-items/{id}")
    public ResponseEntity<ApiResponse<EstimateItemResponse>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody CreateEstimateItemRequest request) {

        return ApiResponseUtil.success(
                "Estimate item updated successfully.",
                service.updateItem(id, request)
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