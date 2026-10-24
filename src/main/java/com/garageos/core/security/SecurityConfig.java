package com.garageos.core.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final DaoAuthenticationProvider authenticationProvider;
    private final com.garageos.core.ratelimit.RateLimitFilter rateLimitFilter;

    @org.springframework.beans.factory.annotation.Value("${springdoc.swagger-ui.enabled:false}")
    private boolean swaggerEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(Customizer.withDefaults())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(
                                jwtAuthenticationEntryPoint))

                .authenticationProvider(authenticationProvider)

                .authorizeHttpRequests(auth -> auth

                        // API documentation is public only where explicitly enabled (SWAGGER_ENABLED=true); in
                        // production it is both switched off in springdoc and not whitelisted here.
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        ).access((authentication, context) ->
                                new org.springframework.security.authorization.AuthorizationDecision(swaggerEnabled))

                        .requestMatchers(
                                "/",
                                // Legacy MVP pages are no longer packaged or whitelisted; only these compliance pages are public.
                                "/privacy-policy.html",
                                "/delete-account.html",
                                "/delete-account-requested.html",
                                "/delete-account",
                                "/privacy-policy",

                                "/favicon.ico",

                                "/api/v1/auth/**",
                                // OTP issue: public for USER_REGISTRATION; the service itself
                                // requires an authenticated user for GARAGE_REGISTRATION.
                                "/api/v1/otp/send",
                                "/api/v1/health/**",

                                // Google Drive OAuth: a browser redirect cannot carry a Bearer header. Only these two
                                // are public, and each is gated (operator token / signed state, see DriveOAuthGuard);
                                // the /test/** diagnostics under the same prefix require SUPER_ADMIN.
                                "/api/v1/media/google/authorize",
                                "/api/v1/media/google/callback",

                                // Emergency Assist is usable before login. Each public route is protected inside the
                                // service (OTP-verified contact, status token, rate limits, server-side eligibility);
                                // the garage-side /api/v1/emergency/garage/** routes stay authenticated.
                                "/api/v1/emergency/verify/**",
                                "/api/v1/emergency/garages/search",
                                "/api/v1/emergency/requests",
                                "/api/v1/emergency/requests/**"
                        ).permitAll()

                        .anyRequest()
                        .authenticated())

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)

                // After the JWT filter so authenticated callers are limited per user, anonymous ones per IP.
                .addFilterAfter(
                        rateLimitFilter,
                        JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }
}