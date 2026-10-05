package com.garageos.modules.otp.service.impl;

import com.garageos.core.enums.otp.OtpPurpose;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.otp.config.OtpProperties;
import com.garageos.modules.otp.dto.response.SendOtpResponse;
import com.garageos.modules.otp.exception.OtpException;
import com.garageos.modules.otp.exception.OtpException.Code;
import com.garageos.modules.otp.service.OtpFlowService;
import com.garageos.modules.otp.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OtpFlowServiceImpl implements OtpFlowService {

    private final OtpService otpService;
    private final OtpProperties props;
    private final UserRepository userRepository;

    @Override
    public SendOtpResponse send(OtpPurpose purpose, String mobile, GarageUserPrincipal principal) {

        if (!otpService.isEnabled()) {
            throw new OtpException(Code.OTP_DISABLED, "OTP verification is not enabled.");
        }

        String destination;
        Long userId = null;

        switch (purpose) {
            case USER_REGISTRATION -> {
                if (mobile == null || mobile.isBlank()) {
                    throw new OtpException(Code.OTP_INVALID_DESTINATION, "Mobile number is required.");
                }
                // Same rule registration itself applies; fail early instead of texting a number
                // that can never register.
                if (userRepository.existsByMobile(mobile)) {
                    throw new BusinessException("Mobile number already exists.");
                }
                destination = mobile;
            }
            case GARAGE_REGISTRATION -> {
                if (principal == null) {
                    throw new AccessDeniedException("Authentication is required.");
                }
                User user = userRepository.findById(principal.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found."));
                destination = user.getMobile();
                userId = user.getId();
            }
            default -> throw new IllegalArgumentException(
                    "OTP purpose " + purpose + " is not supported yet.");
        }

        otpService.send(purpose, destination, userId);

        return SendOtpResponse.builder()
                .maskedDestination(mask(destination))
                .expiresInSeconds(props.getExpiry().toSeconds())
                .resendAfterSeconds(props.getResendCooldown().toSeconds())
                .build();
    }

    private static String mask(String mobile) {
        if (mobile == null || mobile.length() < 4) {
            return "XXXXXX";
        }
        return "XXXXXX" + mobile.substring(mobile.length() - 4);
    }
}
