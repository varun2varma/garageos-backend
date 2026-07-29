package com.garageos.modules.identity.security.jwt;

import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.access-token-expiration}")
    private Duration accessTokenExpiration;

    @Value("${security.jwt.refresh-token-expiration}")
    private Duration refreshTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    public String generateAccessToken(GarageUserPrincipal user) {

        Date now = new Date();

        Date expiry = new Date(
                now.getTime() + accessTokenExpiration.toMillis()
        );

        return Jwts.builder()

                .subject(user.getUsername())

                .claim("userId", user.getId())

                .claim("garageId", user.getGarageId())

                .claim("roles", user.getRoles())

                .issuedAt(now)

                .expiration(expiry)

                .signWith(getSigningKey())

                .compact();
    }

    @Override
    public String generateRefreshToken(GarageUserPrincipal user) {

        Date now = new Date();

        Date expiry = new Date(
                now.getTime() + accessTokenExpiration.toMillis()
        );

        return Jwts.builder()

                .subject(user.getUsername())

                .claim("userId", user.getId())

                .issuedAt(now)

                .expiration(expiry)

                .signWith(getSigningKey())

                .compact();
    }

    @Override
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    @Override
    public Long extractUserId(String token) {
        return getClaims(token)
                .get("userId", Long.class);
    }

    @Override
    public LocalDateTime extractExpiration(String token) {

        return LocalDateTime.ofInstant(
                getClaims(token)
                        .getExpiration()
                        .toInstant(),
                ZoneId.systemDefault()
        );
    }

    @Override
    public boolean isTokenValid(String token) {

        try {

            return extractExpiration(token)
                    .isAfter(LocalDateTime.now());

        } catch (Exception ex) {

            return false;
        }
    }

    @Override
    public boolean isTokenValid(
            String token,
            GarageUserPrincipal user) {

        return extractUsername(token)
                .equals(user.getUsername())
                && isTokenValid(token);
    }

    @Override
    public Duration getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    @Override
    public Duration getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    private Claims getClaims(String token) {

        return Jwts.parser()

                .verifyWith(getSigningKey())

                .build()

                .parseSignedClaims(token)

                .getPayload();
    }

    public String extractToken(HttpServletRequest request) {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }

        return header.substring(7);
    }

}