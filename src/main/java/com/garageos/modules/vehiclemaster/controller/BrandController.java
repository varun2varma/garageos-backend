package com.garageos.modules.vehiclemaster.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.vehiclemaster.dto.request.CreateVehicleBrandRequest;
import com.garageos.modules.vehiclemaster.dto.response.VehicleBrandResponse;
import com.garageos.modules.vehiclemaster.dto.response.VehicleDropdownResponse;
import com.garageos.modules.vehiclemaster.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicle-master/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleBrandResponse>> create(
            @Valid @RequestBody CreateVehicleBrandRequest request) {

        return ApiResponseUtil.created(
                "Vehicle brand created successfully.",
                brandService.create(request)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleBrandResponse>>> getAll() {

        return ApiResponseUtil.success(
                "Vehicle brands fetched successfully.",
                brandService.getAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleBrandResponse>> getById(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Vehicle brand fetched successfully.",
                brandService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleBrandResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateVehicleBrandRequest request) {

        return ApiResponseUtil.success(
                "Vehicle brand updated successfully.",
                brandService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id) {

        brandService.delete(id);

        return ApiResponseUtil.success(
                "Vehicle brand deleted successfully."
        );
    }

    @GetMapping("/dropdown")
    public ResponseEntity<ApiResponse<List<VehicleDropdownResponse>>> getBrands() {

        return ApiResponseUtil.success(
                        "Brands fetched successfully.",
                        brandService.getAllBrands()
        );
    }
}