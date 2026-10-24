package com.garageos.modules.identity.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.identity.service.EmailVerificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated e-mail verification for the caller's own account (authenticated by the default security rule). */
@RestController
@RequestMapping("/api/v1/account/email-verification")
@RequiredArgsConstructor
public class AccountEmailController {

    private final EmailVerificationService service;

    @Getter
    @Setter
    public static class VerifyEmailRequest {
        @NotBlank(message = "OTP is required.")
        @Pattern(regexp = "^\\d{4,8}$", message = "Invalid OTP.")
        private String otp;
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> send(@AuthenticationPrincipal GarageUserPrincipal user) {
        service.sendOwn(user);
        return ApiResponseUtil.success("Verification code sent to your e-mail address.");
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @Valid @RequestBody VerifyEmailRequest request) {
        service.verifyOwn(user, request.getOtp());
        return ApiResponseUtil.success("E-mail address verified.");
    }
}
