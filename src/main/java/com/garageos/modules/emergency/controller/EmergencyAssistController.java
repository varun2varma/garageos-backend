package com.garageos.modules.emergency.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.emergency.dto.request.EmergencyRequests;
import com.garageos.modules.emergency.dto.response.EmergencyResponses;
import com.garageos.modules.emergency.service.EmergencyAssistService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Emergency Assist. The first group of routes is public (reachable before login) and is protected by contact
 * verification, rate limits and server-computed eligibility; the {@code /garage/**} routes are authenticated and
 * garage-scoped. See {@code SecurityConfig} for exactly which paths are public.
 */
@RestController
@RequestMapping("/api/v1/emergency")
@RequiredArgsConstructor
public class EmergencyAssistController {

    private static final String GARAGE_ROLES = "hasAnyRole('OWNER', 'MANAGER', 'SERVICE_ADVISOR')";

    private final EmergencyAssistService service;

    // ---------------------------------------------------------------- public (pre-login)

    @PostMapping("/verify/send")
    public ResponseEntity<ApiResponse<EmergencyResponses.VerificationSent>> send(
            @Valid @RequestBody EmergencyRequests.SendVerification request) {
        return ApiResponseUtil.success("Verification code sent.", service.sendVerification(request.getMobile()));
    }

    @PostMapping("/verify/confirm")
    public ResponseEntity<ApiResponse<EmergencyResponses.VerificationConfirmed>> confirm(
            @Valid @RequestBody EmergencyRequests.ConfirmVerification request) {
        return ApiResponseUtil.success("Number verified.",
                service.confirmVerification(request.getMobile(), request.getOtp()));
    }

    /** POST (not GET) so the precise location is never part of a URL that could be logged. */
    @PostMapping("/garages/search")
    public ResponseEntity<ApiResponse<EmergencyResponses.GarageOptions>> garages(
            @Valid @RequestBody EmergencyRequests.SearchGarages request) {
        return ApiResponseUtil.success("Eligible garages fetched.", service.eligibleGarages(request));
    }

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<EmergencyResponses.RequestView>> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody EmergencyRequests.Create request) {
        return ApiResponseUtil.created("Emergency request sent.", service.create(idempotencyKey, request));
    }

    @GetMapping("/requests/{requestCode}")
    public ResponseEntity<ApiResponse<EmergencyResponses.RequestView>> status(
            @PathVariable String requestCode,
            @RequestHeader(value = "X-Emergency-Token", required = false) String statusToken) {
        return ApiResponseUtil.success("Request fetched.", service.status(requestCode, statusToken));
    }

    @PostMapping("/requests/{requestCode}/cancel")
    public ResponseEntity<ApiResponse<EmergencyResponses.RequestView>> cancel(
            @PathVariable String requestCode,
            @RequestHeader(value = "X-Emergency-Token", required = false) String statusToken) {
        return ApiResponseUtil.success("Request cancelled.", service.cancel(requestCode, statusToken));
    }

    // ---------------------------------------------------------------- garage staff

    @GetMapping("/garage/requests")
    @PreAuthorize(GARAGE_ROLES)
    public ResponseEntity<ApiResponse<List<EmergencyResponses.GarageRequestView>>> garageRequests(
            @AuthenticationPrincipal GarageUserPrincipal user) {
        return ApiResponseUtil.success("Requests fetched.", service.garageRequests(user));
    }

    @PostMapping("/garage/requests/{id}/accept")
    @PreAuthorize(GARAGE_ROLES)
    public ResponseEntity<ApiResponse<EmergencyResponses.GarageRequestView>> accept(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) EmergencyRequests.Decision body) {
        return ApiResponseUtil.success("Request accepted.", service.accept(id, remarks(body), user));
    }

    @PostMapping("/garage/requests/{id}/reject")
    @PreAuthorize(GARAGE_ROLES)
    public ResponseEntity<ApiResponse<EmergencyResponses.GarageRequestView>> reject(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) EmergencyRequests.Decision body) {
        return ApiResponseUtil.success("Request rejected.", service.reject(id, remarks(body), user));
    }

    @PostMapping("/garage/requests/{id}/complete")
    @PreAuthorize(GARAGE_ROLES)
    public ResponseEntity<ApiResponse<EmergencyResponses.GarageRequestView>> complete(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) EmergencyRequests.Decision body) {
        return ApiResponseUtil.success("Request completed.", service.complete(id, remarks(body), user));
    }

    private static String remarks(EmergencyRequests.Decision body) {
        return body == null ? null : body.getRemarks();
    }
}
