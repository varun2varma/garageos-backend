package com.garageos.modules.identity.service;

import com.garageos.modules.identity.dto.request.ChangePasswordRequest;
import com.garageos.modules.identity.dto.request.LoginRequest;
import com.garageos.modules.identity.dto.request.RegisterRequest;
import com.garageos.modules.identity.dto.response.LoginResponse;
import com.garageos.modules.identity.dto.response.RegisterResponse;
import com.garageos.modules.identity.dto.response.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    LoginResponse login(
            LoginRequest request,
            HttpServletRequest servletRequest);

    void logout(HttpServletRequest request);

    void changePassword(ChangePasswordRequest request);

    LoginResponse refreshToken(String refreshToken);

    UserProfileResponse me();

    RegisterResponse register(RegisterRequest request);

}