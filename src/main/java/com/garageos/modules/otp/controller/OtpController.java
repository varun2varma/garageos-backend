package com.garageos.modules.otp.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.otp.dto.request.SendOtpRequest;
import com.garageos.modules.otp.dto.response.SendOtpResponse;
import com.garageos.modules.otp.service.OtpFlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * There is deliberately no standalone "verify" endpoint: the OTP is verified
 * and consumed atomically by the flow it protects (POST /auth/register,
 * POST /garages), which therefore cannot be completed without it.
 */
@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpFlowService otpFlowService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<SendOtpResponse>> send(
            @AuthenticationPrincipal GarageUserPrincipal principal,
            @Valid @RequestBody SendOtpRequest request) {

        return ApiResponseUtil.success(
                "OTP sent.",
                otpFlowService.send(request.getPurpose(), request.getMobile(), principal));
    }
}
