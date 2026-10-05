package com.garageos.modules.otp.repository;

import com.garageos.core.enums.otp.OtpPurpose;
import com.garageos.core.enums.otp.OtpStatus;
import com.garageos.modules.otp.entity.OtpChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {

    /** Row-locked so two concurrent verifications cannot both redeem the same challenge. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpChallenge> findFirstByDestinationAndPurposeAndStatus(
            String destination, OtpPurpose purpose, OtpStatus status);

    Optional<OtpChallenge> findFirstByDestinationAndPurposeOrderByCreatedAtDesc(
            String destination, OtpPurpose purpose);

    long countByDestinationAndCreatedAtAfter(String destination, LocalDateTime after);
}
