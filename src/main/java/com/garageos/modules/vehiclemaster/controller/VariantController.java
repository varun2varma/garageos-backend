package com.garageos.modules.vehiclemaster.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.core.enums.EnumDropdownResponse;
import com.garageos.core.enums.TransmissionType;
import com.garageos.modules.vehiclemaster.dto.request.CreateVehicleVariantRequest;
import com.garageos.modules.vehiclemaster.dto.response.VehicleDropdownResponse;
import com.garageos.modules.vehiclemaster.dto.response.VehicleMasterMetadataResponse;
import com.garageos.modules.vehiclemaster.dto.response.VehicleVariantResponse;
import com.garageos.modules.vehiclemaster.service.VariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicle-master/variants")
@RequiredArgsConstructor
public class VariantController {

    private final VariantService variantService;

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleVariantResponse>> create(
            @Valid @RequestBody CreateVehicleVariantRequest request) {

        return ApiResponseUtil.created(
                "Vehicle variant created successfully.",
                variantService.create(request)
        );
    }

    @GetMapping("/model/{modelId}")
    public ResponseEntity<ApiResponse<List<VehicleVariantResponse>>> getByModel(
            @PathVariable Long modelId) {

        return ApiResponseUtil.success(
                "Vehicle variants fetched successfully.",
                variantService.getByModel(modelId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleVariantResponse>> getById(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Vehicle variant fetched successfully.",
                variantService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleVariantResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateVehicleVariantRequest request) {

        return ApiResponseUtil.success(
                "Vehicle variant updated successfully.",
                variantService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id) {

        variantService.delete(id);

        return ApiResponseUtil.success(
                "Vehicle variant deleted successfully."
        );
    }

    @GetMapping("/dropdown/model/{modelId}")
    public ResponseEntity<ApiResponse<List<VehicleDropdownResponse>>> getVariantsByModel(
            @PathVariable Long modelId) {

        return ApiResponseUtil.success(
                        "Variants fetched successfully.",
                        variantService.getVariantsByModel(modelId)
        );
    }

    @GetMapping("/dropdown/model/{modelId}/transmissions")
    public ResponseEntity<ApiResponse<List<EnumDropdownResponse>>> getTransmissionDropdown(
            @PathVariable Long modelId) {

        return ApiResponseUtil.success(
                "Transmission types fetched successfully.",
                variantService.getTransmissionDropdown(modelId)
        );
    }

    @GetMapping("/metadata")
    public ResponseEntity<ApiResponse<VehicleMasterMetadataResponse>> getMetadata() {

        try {
            VehicleMasterMetadataResponse response = variantService.getMetadata();

            return ApiResponseUtil.success(
                    "Metadata fetched successfully.",
                    response
            );

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/dropdown/model/{modelId}/transmissions/{transmission}/fuel-types")
    public ResponseEntity<ApiResponse<List<EnumDropdownResponse>>> getFuelTypeDropdown(
            @PathVariable Long modelId,
            @PathVariable TransmissionType transmission) {

        return ApiResponseUtil.success(
                "Fuel types fetched successfully.",
                variantService.getFuelTypeDropdown(
                        modelId,
                        transmission)
        );
    }

}