package com.garageos.modules.otp.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.otp.OtpChannel;
import com.garageos.core.enums.otp.OtpPurpose;
import com.garageos.core.enums.otp.OtpStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * One issued OTP. The OTP value is never persisted - only {@code otpHash}, an
 * HMAC bound to the destination and purpose.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "otp_challenge")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OtpChallenge extends BaseEntity {

    @Column(nullable = false, length = 150)
    String destination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    OtpChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    OtpPurpose purpose;

    @Column(name = "user_id")
    Long userId;

    @Column(name = "otp_hash", nullable = false, length = 128)
    String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    OtpStatus status;

    @Column(name = "attempt_count", nullable = false)
    int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    int maxAttempts;

    @Column(nullable = false, length = 30)
    String provider;

    @Column(name = "provider_reference", length = 100)
    String providerReference;

    @Column(name = "expires_at", nullable = false)
    LocalDateTime expiresAt;

    @Column(name = "verified_at")
    LocalDateTime verifiedAt;
}
