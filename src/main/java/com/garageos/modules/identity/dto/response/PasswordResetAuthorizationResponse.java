package com.garageos.modules.identity.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Returned once, after a verified password-reset OTP. The token is single-use and short-lived. */
@Getter
@Builder
public class PasswordResetAuthorizationResponse {

    private String resetToken;
    private long expiresInSeconds;
}
