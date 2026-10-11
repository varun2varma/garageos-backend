package com.garageos.core.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Browser (Flutter Web) access to {@code /api/**}.
 *
 * Only the exact origins listed in {@code garagest.cors.allowed-origins} (comma separated, for example
 * {@code http://localhost:8081,https://app.example.com}) may call the API from a browser. The default is
 * an empty list, i.e. no cross-origin access, exactly as before. A wildcard is never honoured: the API
 * is authenticated, so "any origin" is not a safe setting. Authentication is a Bearer token in the
 * Authorization header, so cookies and credentialed requests are not needed or allowed.
 *
 * Native apps (Android/iOS) and same-origin pages are not affected by CORS.
 */
@Slf4j
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${garagest.cors.allowed-origins:}") String allowedOrigins) {

        List<String> origins = parseOrigins(allowedOrigins);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        if (origins.isEmpty()) {
            return source;
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/api/**", config);

        log.info("[CORS] Browser access enabled for {} configured origin(s) on /api/**.", origins.size());

        return source;
    }

    /** Trims, drops blanks and any wildcard, and removes a trailing slash. Package-visible for tests. */
    static List<String> parseOrigins(String raw) {

        List<String> origins = new ArrayList<>();

        if (raw == null) {
            return origins;
        }

        for (String part : raw.split(",")) {
            String origin = part.trim();

            if (origin.isEmpty()) {
                continue;
            }

            if (origin.contains("*")) {
                log.warn("[CORS] Ignoring wildcard origin '{}': list exact origins instead.", origin);
                continue;
            }

            origins.add(origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin);
        }

        return origins;
    }
}
