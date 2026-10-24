package com.garageos.modules.garage.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.garage.dto.request.CreateGarageRequest;
import com.garageos.modules.garage.dto.request.UpdateGarageLocationRequest;
import com.garageos.modules.garage.dto.response.GarageBrandingResponse;
import com.garageos.modules.garage.dto.response.GarageResponse;
import com.garageos.modules.garage.dto.response.NearbyGarageResponse;
import com.garageos.modules.garage.service.GarageDiscoveryService;
import com.garageos.core.security.GarageScope;
import org.springframework.security.access.AccessDeniedException;
import com.garageos.modules.garage.dto.response.GarageSummaryResponse;
import com.garageos.modules.garage.service.GarageBrandingService;
import com.garageos.modules.garage.service.GarageService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GarageController {

    private final GarageService service;

    private final GarageBrandingService brandingService;

    private final GarageDiscoveryService discoveryService;

    private final GarageScope garageScope;

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
    public ResponseEntity<ApiResponse<GarageSummaryResponse>> getGarage(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Garage fetched successfully.",
                service.getGarage(user, id)
        );
    }

    @PutMapping("/garages/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<GarageResponse>> updateGarage(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody CreateGarageRequest request) {

        return ApiResponseUtil.success(
                "Garage updated successfully.",
                service.updateGarage(user, id, request)
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

    /**
     * Garage branding (name, contact, GSTIN, logo availability), resolved by
     * garageId. Readable by the garage's own staff and by customers who have
     * a job card at that garage (enforced in GarageBrandingServiceImpl).
     */
    @GetMapping("/garages/{id}/branding")
    public ResponseEntity<ApiResponse<GarageBrandingResponse>> getGarageBranding(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Garage branding fetched successfully.",
                brandingService.getBranding(user, id)
        );
    }

    /**
     * Owner-only set/clear of the garage colour palette (primary and secondary, {@code #RRGGBB}; omit or send null to
     * go back to the default theme). Ownership of THIS garage is enforced in GarageBrandingServiceImpl against the
     * authenticated principal, never trusted from the request.
     */
    @PutMapping("/garages/{id}/branding")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<GarageBrandingResponse>> updateGarageTheme(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody com.garageos.modules.garage.dto.request.UpdateBrandingThemeRequest body) {

        return ApiResponseUtil.success(
                "Garage branding updated successfully.",
                brandingService.updateTheme(user, id, body.getPrimaryColor(), body.getSecondaryColor())
        );
    }

    /** Raw logo bytes - authenticated and authorized like branding; never a public path. */
    @GetMapping("/garages/{id}/logo")
    public ResponseEntity<byte[]> getGarageLogo(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long id) {

        GarageBrandingService.LogoContent logo = brandingService.getLogo(user, id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.contentType()))
                .cacheControl(CacheControl.noCache().cachePrivate())
                .body(logo.bytes());
    }

    /**
     * Owner-only add/replace of the garage logo (multipart field "file").
     * Used at registration and later from the owner dashboard. Garage
     * ownership is enforced in GarageBrandingServiceImpl against the
     * authenticated principal, never trusted from the request.
     */
    @PostMapping(value = "/garages/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<GarageBrandingResponse>> updateGarageLogo(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        return ApiResponseUtil.success(
                "Garage logo updated successfully.",
                brandingService.updateLogo(user, id, file)
        );
    }

    @DeleteMapping("/garages/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteGarage(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long id) {

        service.deleteGarage(user, id);

        return ApiResponseUtil.success(
                "Garage deleted successfully."
        );
    }

    /**
     * ACTIVE garage directory for non-customer flows (e.g. an employee
     * choosing a garage to join). Customers do NOT get the whole directory -
     * they discover garages through /garages/nearby or their own My Garages.
     */
    @GetMapping("/garages")
    public ResponseEntity<ApiResponse<List<GarageSummaryResponse>>> getAllGarages(
            @AuthenticationPrincipal GarageUserPrincipal user) {

        if (garageScope.isCustomerOnly(user)) {
            throw new AccessDeniedException(
                    "Use nearby garage discovery to find a garage.");
        }

        return ApiResponseUtil.success(
                "Garages fetched successfully.",
                service.getAllGarages()
        );

    }

    /**
     * Customer garage discovery: ACTIVE garages with coordinates within
     * radiusKm (configurable default) of the supplied point, nearest first.
     */
    @GetMapping("/garages/nearby")
    public ResponseEntity<ApiResponse<List<NearbyGarageResponse>>> getNearbyGarages(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false) Double radiusKm) {

        return ApiResponseUtil.success(
                "Nearby garages fetched successfully.",
                discoveryService.findNearby(latitude, longitude, radiusKm)
        );
    }

}
