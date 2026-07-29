package com.garageos.modules.identity.security.jwt;

import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.time.LocalDateTime;

public interface JwtService {

    String generateAccessToken(GarageUserPrincipal user);

    String generateRefreshToken(GarageUserPrincipal user);

    String extractUsername(String token);

    Long extractUserId(String token);

    LocalDateTime extractExpiration(String token);

    boolean isTokenValid(String token);

    boolean isTokenValid(String token, GarageUserPrincipal user);

    Duration getAccessTokenExpiration();

    Duration getRefreshTokenExpiration();

    String extractToken(HttpServletRequest httpServletRequest);
}