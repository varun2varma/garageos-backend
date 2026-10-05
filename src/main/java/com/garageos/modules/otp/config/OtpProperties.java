package com.garageos.modules.otp.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * OTP configuration. Everything comes from environment variables through
 * application.properties placeholders; no credential is defaulted or committed.
 *
 * {@code enabled=false} (the default) keeps registration exactly as it was
 * before OTP existed, so the feature can be rolled out per environment.
 */
@Getter
@Component
public class OtpProperties {

    private final boolean enabled;
    private final String provider;
    private final String hashSecret;
    private final Duration expiry;
    private final int maxAttempts;
    private final Duration resendCooldown;
    private final int maxSendsPerHour;
    private final int length;

    private final String smsBaseUrl;
    private final String smsCustomerId;
    private final String smsPassword;
    private final String smsCountryCode;
    private final Duration smsTimeout;

    public OtpProperties(
            @Value("${otp.enabled:false}") boolean enabled,
            @Value("${otp.provider:SMS}") String provider,
            @Value("${otp.hash-secret:}") String hashSecret,
            @Value("${otp.expiry-minutes:5}") long expiryMinutes,
            @Value("${otp.max-attempts:5}") int maxAttempts,
            @Value("${otp.resend-cooldown-seconds:60}") long resendCooldownSeconds,
            @Value("${otp.max-sends-per-hour:5}") int maxSendsPerHour,
            @Value("${otp.length:6}") int length,
            @Value("${otp.sms.base-url:https://cpaas.messagecentral.com}") String smsBaseUrl,
            @Value("${otp.sms.customer-id:}") String smsCustomerId,
            @Value("${otp.sms.password:}") String smsPassword,
            @Value("${otp.sms.country-code:91}") String smsCountryCode,
            @Value("${otp.sms.timeout-seconds:8}") long smsTimeoutSeconds) {
        this.enabled = enabled;
        this.provider = provider;
        this.hashSecret = hashSecret;
        this.expiry = Duration.ofMinutes(expiryMinutes);
        this.maxAttempts = maxAttempts;
        this.resendCooldown = Duration.ofSeconds(resendCooldownSeconds);
        this.maxSendsPerHour = maxSendsPerHour;
        this.length = length;
        this.smsBaseUrl = smsBaseUrl == null ? "" : smsBaseUrl.replaceAll("/+$", "");
        this.smsCustomerId = smsCustomerId;
        this.smsPassword = smsPassword;
        this.smsCountryCode = smsCountryCode;
        this.smsTimeout = Duration.ofSeconds(smsTimeoutSeconds);
    }
}
