package com.garageos.modules.identity.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.identity.dto.request.AccountDeletionRequestForm;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final HttpServletRequest httpServletRequest;
    private final com.garageos.modules.identity.security.PasswordRecoveryThrottle recoveryThrottle;

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

        // A throttled request is answered exactly like an accepted one (204), so throttling reveals nothing.
        String ip = com.garageos.modules.identity.security.ClientIpResolver.resolve(httpServletRequest);
        if (recoveryThrottle.allowSend(ip, request.getIdentifier())) {
            authService.forgotPassword(request);
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * SMS-OTP password recovery, step 2: verify the OTP sent to the account's registered mobile. Returns a
     * short-lived single-use reset authorization to present to {@code /reset-password}.
     */
    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<com.garageos.modules.identity.dto.response.PasswordResetAuthorizationResponse> verifyResetOtp(
            @Valid @RequestBody com.garageos.modules.identity.dto.request.VerifyResetOtpRequest request) {

        String ip = com.garageos.modules.identity.security.ClientIpResolver.resolve(httpServletRequest);
        if (!recoveryThrottle.allowVerify(ip, request.getIdentifier())) {
            throw new com.garageos.modules.otp.exception.OtpException(
                    com.garageos.modules.otp.exception.OtpException.Code.OTP_RATE_LIMITED,
                    "Too many attempts. Please try again later.");
        }

        return ResponseEntity.ok(authService.verifyPasswordResetOtp(request));
    }

    /** Authenticated: verify the mobile number on the caller's own account (send the OTP with purpose MOBILE_VERIFICATION). */
    @PostMapping("/mobile/verify")
    public ResponseEntity<Void> verifyMobile(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            com.garageos.modules.identity.security.principal.GarageUserPrincipal principal,
            @Valid @RequestBody com.garageos.modules.identity.dto.request.VerifyMobileRequest request) {

        authService.verifyOwnMobile(principal.getId(), request.getOtp());

        return ResponseEntity.noContent().build();
    }

    /** Mission backlog #13 — forgot password, final step: new password + the reset authorization. */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        String ip = com.garageos.modules.identity.security.ClientIpResolver.resolve(httpServletRequest);
        if (!recoveryThrottle.allowReset(ip)) {
            throw new com.garageos.modules.otp.exception.OtpException(
                    com.garageos.modules.otp.exception.OtpException.Code.OTP_RATE_LIMITED,
                    "Too many attempts. Please try again later.");
        }

        authService.resetPassword(request);

        return ResponseEntity.noContent().build();
    }

    /**
     * Self-service account deletion. Identifies the account to delete from
     * the caller's own JWT/SecurityContext (never a client-supplied id), so
     * a caller can only ever delete their own account. Idempotent — see
     * AuthServiceImpl.deleteAccount's own doc comment.
     */
    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount() {

        authService.deleteAccount();

        return ResponseEntity.noContent().build();
    }

    /**
     * Google Play external account-deletion requirement: the plain-HTML,
     * no-JS form on the public /delete-account page posts here directly
     * (standard browser form submission, application/x-www-form-urlencoded -
     * no fetch/AJAX). Publicly reachable (covered by SecurityConfig's
     * existing "/api/v1/auth/**" permitAll matcher - no security-config
     * change needed for this endpoint itself), but see
     * AuthServiceImpl.requestAccountDeletion's own doc comment: this never
     * deletes anything by itself, only records a request. Redirects
     * (rather than returning JSON) since the caller is a browser following
     * a plain form submission, to a static, deliberately generic
     * confirmation page that reveals nothing about whether the submitted
     * identifier matched a real account.
     */
    @PostMapping(value = "/account-deletion-request", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> requestAccountDeletion(
            @Valid @ModelAttribute AccountDeletionRequestForm form,
            HttpServletRequest request) {

        authService.requestAccountDeletion(form.getIdentifier(),
                com.garageos.modules.identity.security.ClientIpResolver.resolve(request));

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/delete-account-requested.html"))
                .build();
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