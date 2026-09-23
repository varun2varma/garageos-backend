package com.garageos.modules.identity.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.identity.dto.request.ChangePasswordRequest;
import com.garageos.modules.identity.dto.request.ForgotPasswordRequest;
import com.garageos.modules.identity.dto.request.LoginRequest;
import com.garageos.modules.identity.dto.request.RegisterRequest;
import com.garageos.modules.identity.dto.request.ResetPasswordRequest;
import com.garageos.modules.identity.dto.request.UpdateProfileRequest;
import com.garageos.modules.identity.dto.response.LoginResponse;
import com.garageos.modules.identity.dto.response.RegisterResponse;
import com.garageos.modules.identity.dto.response.UserProfileResponse;
import com.garageos.modules.identity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final HttpServletRequest httpServletRequest;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        return ApiResponseUtil.success(
                "Login successful.",
                authService.login(request, httpServletRequest)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me() {
        return ResponseEntity.ok(authService.me());
    }

    /**
     * Mission backlog #14 — self-service profile edit. Matches /me's own
     * bare-DTO convention (no ApiResponse envelope), since this is the
     * same resource.
     */
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request) {

        authService.logout(request);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        authService.changePassword(request);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid
            @RequestBody RegisterRequest request) {

        return ApiResponseUtil.created(
                "Registration successful.",
                authService.register(request)
        );
    }

    /**
     * Mission backlog #13 — forgot password, step 1. Always 204, never
     * reveals whether the identifier matched an account (see
     * AuthServiceImpl.forgotPassword's own doc comment).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request);

        return ResponseEntity.noContent().build();
    }

    /** Mission backlog #13 — forgot password, step 2. */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @RequestBody Map<String, String> request) {

        String refreshToken = request.get("refreshToken");

        return ApiResponseUtil.success(
                "Token refreshed successfully.",
                authService.refreshToken(refreshToken)
        );
    }

}