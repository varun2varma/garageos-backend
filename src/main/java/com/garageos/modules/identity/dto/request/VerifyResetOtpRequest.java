package com.garageos.modules.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyResetOtpRequest {

    /** Username, email, or mobile - the same identifier used to request the OTP. */
    @NotBlank(message = "Username, email, or mobile is required.")
    private String identifier;

    @NotBlank(message = "OTP is required.")
    @Size(max = 12, message = "OTP is invalid.")
    private String otp;
}
