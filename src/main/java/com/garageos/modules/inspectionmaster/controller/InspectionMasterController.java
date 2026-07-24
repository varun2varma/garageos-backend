package com.garageos.modules.inspectionmaster.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import com.garageos.modules.inspectionmaster.dto.request.CreateInspectionMasterRequest;
import com.garageos.modules.inspectionmaster.dto.response.InspectionMasterResponse;
import com.garageos.modules.inspectionmaster.service.InspectionMasterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inspection-master")
@RequiredArgsConstructor
public class InspectionMasterController {

    private final InspectionMasterService service;

    @PostMapping
    public ResponseEntity<ApiResponse<InspectionMasterResponse>> createInspectionMaster(
            @Valid @RequestBody CreateInspectionMasterRequest request) {

        return ApiResponseUtil.created(
                "Inspection Master created successfully.",
                service.createInspectionMaster(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InspectionMasterResponse>> getInspectionMaster(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Inspection Master fetched successfully.",
                service.getInspectionMaster(id));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InspectionMasterResponse>>> getAllInspectionMasters(

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return ApiResponseUtil.success(
                "Inspection Masters fetched successfully.",
                service.getAllInspectionMasters(
                        page,
                        size,
                        sortBy,
                        direction));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InspectionMasterResponse>> updateInspectionMaster(
            @PathVariable Long id,
            @Valid @RequestBody CreateInspectionMasterRequest request) {

        return ApiResponseUtil.success(
                "Inspection Master updated successfully.",
                service.updateInspectionMaster(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInspectionMaster(
            @PathVariable Long id) {

        service.deleteInspectionMaster(id);

        return ApiResponseUtil.success(
                "Inspection Master deleted successfully.");
    }


    @GetMapping("/search")
    public ResponseEntity<ApiResponse<InspectionMasterResponse>> searchInspectionMaster(

            @RequestParam String make,

            @RequestParam String model,

            @RequestParam String variant,

            @RequestParam FuelType fuelType,

            @RequestParam TransmissionType transmissionType,

            @RequestParam Integer year,

            @RequestParam Integer odometer) {

        return ApiResponseUtil.success(
                "Inspection Master fetched successfully.",
                service.getInspectionMasterByVehicle(
                        make,
                        model,
                        variant,
                        fuelType,
                        transmissionType,
                        year,
                        odometer));
    }

}