package com.garageos.modules.delivery.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.delivery.dto.request.CreateDeliveryRequest;
import com.garageos.modules.delivery.dto.response.DeliveryResponse;
import com.garageos.modules.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService service;

    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryResponse>> createDelivery(
            @Valid @RequestBody CreateDeliveryRequest request) {

        return ApiResponseUtil.created(
                "Vehicle delivered successfully.",
                service.createDelivery(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryResponse>> getDelivery(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Delivery fetched successfully.",
                service.getDelivery(id));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DeliveryResponse>>> getAllDeliveries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return ApiResponseUtil.success(
                "Deliveries fetched successfully.",
                service.getAllDeliveries(page, size, sortBy, direction));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryResponse>> updateDelivery(
            @PathVariable Long id,
            @Valid @RequestBody CreateDeliveryRequest request) {

        return ApiResponseUtil.success(
                "Delivery updated successfully.",
                service.updateDelivery(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDelivery(
            @PathVariable Long id) {

        service.deleteDelivery(id);

        return ApiResponseUtil.success(
                "Delivery deleted successfully.");
    }
}