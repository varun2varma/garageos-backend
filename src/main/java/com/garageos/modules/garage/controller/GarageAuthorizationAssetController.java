package com.garageos.modules.garage.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.garage.service.GarageAuthorizationAssetService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Owner-only management of the garage authorized signature and stamp. There is deliberately NO route that returns
 * the image bytes: the assets are private and only the final invoice PDF embeds them.
 */
@RestController
@RequestMapping("/api/v1/garages/{garageId}/authorization")
@RequiredArgsConstructor
public class GarageAuthorizationAssetController {

    private final GarageAuthorizationAssetService service;

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<GarageAuthorizationAssetService.Status>> status(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long garageId) {

        return ApiResponseUtil.success("Authorization status fetched.", service.status(user, garageId));
    }

    /** One file holding both the signature and the stamp (the separate endpoints below remain for existing clients). */
    @PostMapping(value = "/combined", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<GarageAuthorizationAssetService.Status>> uploadCombined(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long garageId,
            @RequestParam("file") MultipartFile file) {

        return ApiResponseUtil.success("Signature and stamp updated.",
                service.upload(user, garageId, GarageAuthorizationAssetService.Kind.COMBINED, file));
    }

    @PostMapping(value = "/signature", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<GarageAuthorizationAssetService.Status>> uploadSignature(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long garageId,
            @RequestParam("file") MultipartFile file) {

        return ApiResponseUtil.success("Signature updated.",
                service.upload(user, garageId, GarageAuthorizationAssetService.Kind.SIGNATURE, file));
    }

    @PostMapping(value = "/stamp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<GarageAuthorizationAssetService.Status>> uploadStamp(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long garageId,
            @RequestParam("file") MultipartFile file) {

        return ApiResponseUtil.success("Stamp updated.",
                service.upload(user, garageId, GarageAuthorizationAssetService.Kind.STAMP, file));
    }
}
