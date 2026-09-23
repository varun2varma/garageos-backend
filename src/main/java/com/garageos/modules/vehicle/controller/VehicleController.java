package com.garageos.modules.vehicle.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.vehicle.dto.request.CreateVehicleRequest;
import com.garageos.modules.vehicle.dto.request.SetRcVerificationRequest;
import com.garageos.modules.vehicle.dto.response.VehicleResponse;
import com.garageos.modules.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService service;

    /**
     * Mission backlog #1 — RC verification is a garage-staff action, not
     * a technician or customer one. Matches the ASSIGNMENT_ROLES pattern
     * used elsewhere (RepairTaskController, JobAssignmentController).
     */
    private static final String RC_VERIFICATION_ROLES = """
            hasAnyRole(
                'MANAGER',
                'SERVICE_ADVISOR',
                'OWNER'
            )
            """;

    @PutMapping("/{id}/rc-verification")
    @PreAuthorize(RC_VERIFICATION_ROLES)
    public ResponseEntity<ApiResponse<VehicleResponse>> setRcVerification(
            @PathVariable Long id,
            @Valid @RequestBody SetRcVerificationRequest request) {

        return ApiResponseUtil.success(
                "RC verification status updated successfully.",
                service.setRcVerification(id, request.getStatus(), request.getDocumentReference())
        );
    }

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