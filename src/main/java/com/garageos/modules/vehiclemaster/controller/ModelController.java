package com.garageos.modules.vehiclemaster.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.vehiclemaster.dto.request.CreateVehicleModelRequest;
import com.garageos.modules.vehiclemaster.dto.response.VehicleDropdownResponse;
import com.garageos.modules.vehiclemaster.dto.response.VehicleModelResponse;
import com.garageos.modules.vehiclemaster.service.ModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicle-master/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleModelResponse>> create(
            @Valid @RequestBody CreateVehicleModelRequest request) {

        return ApiResponseUtil.created(
                "Vehicle model created successfully.",
                modelService.create(request)
        );
    }

    @GetMapping("/brand/{brandId}")
    public ResponseEntity<ApiResponse<List<VehicleModelResponse>>> getByBrand(
            @PathVariable Long brandId) {

        return ApiResponseUtil.success(
                "Vehicle models fetched successfully.",
                modelService.getByBrand(brandId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleModelResponse>> getById(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Vehicle model fetched successfully.",
                modelService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleModelResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateVehicleModelRequest request) {

        return ApiResponseUtil.success(
                "Vehicle model updated successfully.",
                modelService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id) {

        modelService.delete(id);

        return ApiResponseUtil.success(
                "Vehicle model deleted successfully."
        );
    }

    @GetMapping("/dropdown/brand/{brandId}")
    public ResponseEntity<ApiResponse<List<VehicleDropdownResponse>>> getModelsByBrand(
            @PathVariable Long brandId) {

        return ApiResponseUtil.success(
                        "Models fetched successfully.",
                        modelService.getModelsByBrand(brandId)
        );
    }

}