package com.garageos.modules.otp.service.impl;

import com.garageos.core.enums.otp.OtpPurpose;
import com.garageos.core.enums.otp.OtpStatus;
import com.garageos.modules.otp.config.OtpProperties;
import com.garageos.modules.otp.entity.OtpChallenge;
import com.garageos.modules.otp.exception.OtpException;
import com.garageos.modules.otp.exception.OtpException.Code;
import com.garageos.modules.otp.provider.OtpProvider;
import com.garageos.modules.otp.repository.OtpChallengeRepository;
import com.garageos.modules.otp.service.OtpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class OtpServiceImpl implements OtpService {

    private static final String HMAC = "HmacSHA256";

    private final OtpChallengeRepository repository;
    private final OtpProperties props;
    private final List<OtpProvider> providers;
    private final SecureRandom random = new SecureRandom();

    public OtpServiceImpl(OtpChallengeRepository repository, OtpProperties props, List<OtpProvider> providers) {
        this.repository = repository;
        this.props = props;
        this.providers = providers;
    }

    @Override
    public boolean isEnabled() {
        return props.isEnabled();
    }

    // -------------------------------------------------------------------- send

    @Override
    @Transactional
    public void send(OtpPurpose purpose, String destination, Long userId) {

        if (!props.isEnabled()) {
            throw new OtpException(Code.OTP_DISABLED, "OTP verification is not enabled.");
        }

        if (destination == null || !destination.matches("^[6-9]\\d{9}$")) {
            throw new OtpException(Code.OTP_INVALID_DESTINATION, "Invalid mobile number.");
        }

        OtpProvider provider = provider();

        if (!provider.isConfigured() || props.getHashSecret().isBlank()) {
            throw new OtpException(Code.OTP_NOT_CONFIGURED,
                    "OTP delivery is not configured. Please try again later.");
        }

        LocalDateTime now = LocalDateTime.now();

        Optional<OtpChallenge> latest =
                repository.findFirstByDestinationAndPurposeOrderByCreatedAtDesc(destination, purpose);

        if (latest.isPresent()) {
            long waitSeconds = props.getResendCooldown().minus(
                    Duration.between(latest.get().getCreatedAt(), now)).getSeconds();
            if (waitSeconds > 0) {
                throw new OtpException(Code.OTP_RESEND_TOO_SOON,
                        "Please wait " + waitSeconds + " seconds before requesting another OTP.");
            }
        }

        if (repository.countByDestinationAndCreatedAtAfter(destination, now.minusHours(1))
                >= props.getMaxSendsPerHour()) {
            throw new OtpException(Code.OTP_RATE_LIMITED,
                    "Too many OTP requests. Please try again after some time.");
        }

        repository.findFirstByDestinationAndPurposeAndStatus(destination, purpose, OtpStatus.PENDING)
                .ifPresent(old -> {
                    old.setStatus(OtpStatus.SUPERSEDED);
                    repository.saveAndFlush(old);
                });

        // A self-validating provider (Message Central VerifyNow) generates and
        // checks the code itself; we then hold no OTP, only a placeholder HMAC
        // (the column is NOT NULL) and the provider's verification reference.
        boolean providerChecksCode = provider.validatesCodeItself();
        String otp = providerChecksCode ? null : generate();

        OtpChallenge challenge = new OtpChallenge();
        challenge.setDestination(destination);
        challenge.setChannel(provider.channel());
        challenge.setPurpose(purpose);
        challenge.setUserId(userId);
        challenge.setOtpHash(hash(purpose, destination,
                providerChecksCode ? "PROVIDER-VALIDATED|" + java.util.UUID.randomUUID() : otp));
        challenge.setStatus(OtpStatus.PENDING);
        challenge.setAttemptCount(0);
        challenge.setMaxAttempts(props.getMaxAttempts());
        challenge.setProvider(provider.name());
        challenge.setExpiresAt(now.plus(props.getExpiry()));

        try {
            repository.saveAndFlush(challenge);
        } catch (DataIntegrityViolationException e) {
            // A concurrent request created the live challenge first.
            throw new OtpException(Code.OTP_RESEND_TOO_SOON,
                    "An OTP was just sent. Please wait before requesting another.");
        }

        // Last step: if delivery fails the transaction rolls back and no
        // undeliverable challenge is left behind.
        String reference = provider.send(destination, otp, purpose);
        challenge.setProviderReference(reference);
        repository.save(challenge);

        log.info("[OTP] Issued. purpose={}, channel={}, challengeId={}",
                purpose, provider.channel(), challenge.getId());
    }

    // ------------------------------------------------------------------ verify

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = OtpException.class)
    public void verifyAndConsume(OtpPurpose purpose, String destination, String otp) {

        if (otp == null || otp.isBlank()) {
            throw new OtpException(Code.OTP_REQUIRED, "OTP is required.");
        }

        if (props.getHashSecret().isBlank()) {
            throw new OtpException(Code.OTP_NOT_CONFIGURED,
                    "OTP verification is not configured. Please try again later.");
        }

        OtpChallenge challenge = repository
                .findFirstByDestinationAndPurposeAndStatus(destination, purpose, OtpStatus.PENDING)
                .orElse(null);

        if (challenge == null) {
            Optional<OtpChallenge> latest =
                    repository.findFirstByDestinationAndPurposeOrderByCreatedAtDesc(destination, purpose);

            if (latest.isPresent()) {
                switch (latest.get().getStatus()) {
                    case VERIFIED -> throw new OtpException(Code.OTP_ALREADY_VERIFIED,
                            "This OTP has already been used. Please request a new one.");
                    case LOCKED -> throw new OtpException(Code.OTP_TOO_MANY_ATTEMPTS,
                            "Too many incorrect attempts. Please request a new OTP.");
                    case EXPIRED -> throw new OtpException(Code.OTP_EXPIRED,
                            "This OTP has expired. Please request a new one.");
                    default -> { }
                }
            }
            throw new OtpException(Code.OTP_INVALID, "Invalid OTP. Please request a new one.");
        }

        if (challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
            challenge.setStatus(OtpStatus.EXPIRED);
            repository.save(challenge);
            throw new OtpException(Code.OTP_EXPIRED, "This OTP has expired. Please request a new one.");
        }

        if (challenge.getAttemptCount() >= challenge.getMaxAttempts()) {
            challenge.setStatus(OtpStatus.LOCKED);
            repository.save(challenge);
            throw new OtpException(Code.OTP_TOO_MANY_ATTEMPTS,
                    "Too many incorrect attempts. Please request a new OTP.");
        }

        boolean matches;
        OtpProvider owner = providerOf(challenge);

        if (owner != null && owner.validatesCodeItself()) {

            switch (owner.validate(challenge.getProviderReference(), destination, otp.trim())) {
                case VALID -> matches = true;
                case WRONG -> matches = false;
                case EXPIRED -> {
                    challenge.setStatus(OtpStatus.EXPIRED);
                    repository.save(challenge);
                    throw new OtpException(Code.OTP_EXPIRED, "This OTP has expired. Please request a new one.");
                }
                case ALREADY_VERIFIED -> throw new OtpException(Code.OTP_ALREADY_VERIFIED,
                        "This OTP has already been used. Please request a new one.");
                default -> throw new OtpException(Code.OTP_PROVIDER_FAILURE,
                        "Could not verify the OTP right now. Please try again.");
            }

        } else {
            matches = MessageDigest.isEqual(
                    hash(purpose, destination, otp.trim()).getBytes(StandardCharsets.UTF_8),
                    challenge.getOtpHash().getBytes(StandardCharsets.UTF_8));
        }

        if (!matches) {
            challenge.setAttemptCount(challenge.getAttemptCount() + 1);

            if (challenge.getAttemptCount() >= challenge.getMaxAttempts()) {
                challenge.setStatus(OtpStatus.LOCKED);
                repository.save(challenge);
                throw new OtpException(Code.OTP_TOO_MANY_ATTEMPTS,
                        "Too many incorrect attempts. Please request a new OTP.");
            }

            repository.save(challenge);
            throw new OtpException(Code.OTP_INVALID, "Incorrect OTP.");
        }

        challenge.setStatus(OtpStatus.VERIFIED);
        challenge.setVerifiedAt(LocalDateTime.now());
        repository.save(challenge);
    }

    // ----------------------------------------------------------------- helpers

    private OtpProvider provider() {
        return providers.stream()
                .filter(p -> p.channel().name().equalsIgnoreCase(props.getProvider()))
                .findFirst()
                .orElseThrow(() -> new OtpException(Code.OTP_NOT_CONFIGURED,
                        "OTP delivery is not configured. Please try again later."));
    }

    /** The provider that issued this challenge (null if it is no longer registered). */
    private OtpProvider providerOf(OtpChallenge challenge) {
        return providers.stream()
                .filter(p -> p.channel() == challenge.getChannel() && p.name().equals(challenge.getProvider()))
                .findFirst()
                .orElse(null);
    }

    private String generate() {
        int bound = (int) Math.pow(10, props.getLength());
        return String.format("%0" + props.getLength() + "d", random.nextInt(bound));
    }

    /** HMAC over purpose|destination|otp so a hash for one purpose or number is useless for another. */
    private String hash(OtpPurpose purpose, String destination, String otp) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(props.getHashSecret().getBytes(StandardCharsets.UTF_8), HMAC));
            byte[] digest = mac.doFinal(
                    (purpose.name() + "|" + destination + "|" + otp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("OTP hashing unavailable", e);
        }
    }
}
