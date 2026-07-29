package com.garageos.modules.identity.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String accessToken;

    private String refreshToken;

    private Long expiresIn;

    private Boolean firstLogin;

    private UserProfileResponse user;

}