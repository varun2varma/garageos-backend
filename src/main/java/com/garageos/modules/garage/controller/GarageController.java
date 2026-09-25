package com.garageos.modules.garage.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.garage.dto.request.CreateGarageRequest;
import com.garageos.modules.garage.dto.request.UpdateGarageLocationRequest;
import com.garageos.modules.garage.dto.response.GarageResponse;
import com.garageos.modules.garage.service.GarageService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * Dedicated add/edit-location endpoint (OWNER-only), separate from the
     * full-object updateGarage() above - lets an owner set or change just
     * a garage's map coordinates after registration (register_garage_screen
     * already lets this be skipped at creation time - "You can skip this
     * and set it later" - but no "set it later" surface existed until
     * now). Ownership is enforced in GarageServiceImpl.updateGarageLocation
     * against the authenticated principal's garageId, never trusted from
     * the request body.
     */
    @PutMapping("/garages/{id}/location")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<GarageResponse>> updateGarageLocation(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateGarageLocationRequest request) {

        return ApiResponseUtil.success(
                "Garage location updated successfully.",
                service.updateGarageLocation(user, id, request)
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

    @GetMapping("/garages")
    public ResponseEntity<ApiResponse<List<GarageResponse>>> getAllGarages() {

        return ApiResponseUtil.success(
                "Garages fetched successfully.",
                service.getAllGarages()
        );

    }

}