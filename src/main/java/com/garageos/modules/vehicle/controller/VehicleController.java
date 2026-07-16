package com.garageos.modules.vehicle.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.vehicle.dto.request.CreateVehicleRequest;
import com.garageos.modules.vehicle.dto.response.VehicleResponse;
import com.garageos.modules.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService service;

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(
            @Valid @RequestBody CreateVehicleRequest request) {

        VehicleResponse response = service.createVehicle(request);

        return ApiResponseUtil.created(
                "Vehicle created successfully.",
                response
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicle(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Vehicle fetched successfully.",
                service.getVehicle(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody CreateVehicleRequest request) {

        return ApiResponseUtil.success(
                "Vehicle updated successfully.",
                service.updateVehicle(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(
            @PathVariable Long id) {

        service.deleteVehicle(id);

        return ApiResponseUtil.success(
                "Vehicle deleted successfully."
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicleByRegistrationNumber(
            @RequestParam String registrationNumber) {

        return ApiResponseUtil.success(
                "Vehicle fetched successfully.",
                service.getVehicleByRegistrationNumber(registrationNumber)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<VehicleResponse>>> getAllVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return ApiResponseUtil.success(
                "Vehicles fetched successfully.",
                service.getAllVehicles(page, size, sortBy, direction)
        );
    }
}