package com.garageos.modules.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyMobileRequest {

    @NotBlank(message = "OTP is required.")
    @Size(max = 12, message = "OTP is invalid.")
    private String otp;
}
