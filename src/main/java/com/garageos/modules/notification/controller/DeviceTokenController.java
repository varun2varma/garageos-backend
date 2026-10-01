package com.garageos.modules.notification.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.notification.dto.DeviceTokenResponse;
import com.garageos.modules.notification.dto.RegisterDeviceRequest;
import com.garageos.modules.notification.dto.UnregisterDeviceRequest;
import com.garageos.modules.notification.service.DeviceTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Device (FCM token) registration. The user is always the authenticated
 * principal - never a client-supplied id.
 */
@RestController
@RequestMapping("/api/v1/notifications/devices")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping
    public ResponseEntity<ApiResponse<DeviceTokenResponse>> register(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @Valid @RequestBody RegisterDeviceRequest request) {

        return ApiResponseUtil.success(
                "Device registered successfully.",
                deviceTokenService.register(user.getId(), request));
    }

    /** Token travels in the body (not the URL) so it never lands in access logs. */
    @PostMapping("/unregister")
    public ResponseEntity<ApiResponse<Void>> unregister(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @Valid @RequestBody UnregisterDeviceRequest request) {

        deviceTokenService.unregister(user.getId(), request.token());
        return ApiResponseUtil.success("Device unregistered.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> unregisterById(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long id) {

        deviceTokenService.unregisterById(user.getId(), id);
        return ApiResponseUtil.success("Device unregistered.");
    }
}
