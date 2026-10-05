package com.garageos.modules.otp.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Never contains the OTP. */
@Getter
@Builder
public class SendOtpResponse {

    /** The number the OTP was sent to, masked (e.g. XXXXXX3210). */
    private String maskedDestination;

    private long expiresInSeconds;

    private long resendAfterSeconds;
}
