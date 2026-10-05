package com.garageos.modules.otp.dto.request;

import com.garageos.core.enums.otp.OtpPurpose;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendOtpRequest {

    @NotNull(message = "Purpose is required.")
    private OtpPurpose purpose;

    /**
     * Required for USER_REGISTRATION. Ignored for GARAGE_REGISTRATION, where
     * the destination is always the authenticated user's own registered mobile.
     */
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid mobile number.")
    private String mobile;
}
