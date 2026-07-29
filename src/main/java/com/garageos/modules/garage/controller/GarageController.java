package com.garageos.modules.garage.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.garage.dto.request.CreateGarageRequest;
import com.garageos.modules.garage.dto.response.GarageResponse;
import com.garageos.modules.garage.service.GarageService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GarageController {

    private final GarageService service;

    @PostMapping("/garages")
    public ResponseEntity<ApiResponse<GarageResponse>> createGarage(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @Valid @RequestBody CreateGarageRequest request) {

        return ApiResponseUtil.created(
                "Garage created successfully.",
                service.createGarage(user.getId(), request)
        );
    }

    @GetMapping("/garages/{id}")
    public ResponseEntity<ApiResponse<GarageResponse>> getGarage(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Garage fetched successfully.",
                service.getGarage(id)
        );
    }

    @PutMapping("/garages/{id}")
    public ResponseEntity<ApiResponse<GarageResponse>> updateGarage(
            @PathVariable Long id,
            @Valid @RequestBody CreateGarageRequest request) {

        return ApiResponseUtil.success(
                "Garage updated successfully.",
                service.updateGarage(id, request)
        );
    }

    @DeleteMapping("/garages/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGarage(
            @PathVariable Long id) {

        service.deleteGarage(id);

        return ApiResponseUtil.success(
                "Garage deleted successfully."
        );
    }

}