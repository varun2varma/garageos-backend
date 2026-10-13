package com.garageos.modules.handover.service.impl;

import com.garageos.core.enums.audit.AuditEventType;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.enums.navigation.HandoverStatus;
import com.garageos.core.enums.navigation.TripLeg;
import com.garageos.core.enums.navigation.TripType;
import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.audit.service.AuditService;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.handover.dto.response.HandoverCodeResponse;
import com.garageos.modules.handover.dto.response.HandoverStatusResponse;
import com.garageos.modules.handover.entity.VehicleHandover;
import com.garageos.modules.handover.repository.VehicleHandoverRepository;
import com.garageos.modules.handover.service.HandoverService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.navigation.entity.NavigationRequest;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.repository.NavigationTripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import com.garageos.modules.notification.event.NotificationEvent;
import com.garageos.modules.notification.event.NotificationEventPublisher;
import com.garageos.modules.notification.event.NotificationFacts;
import com.garageos.core.enums.notification.NotificationEventType;
import com.garageos.modules.notification.event.NotificationEvents;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Secure vehicle handover confirmation for a NavigationTrip's pickup or
 * delivery leg. The backend is authoritative end to end: codes are
 * generated here with SecureRandom, hashed with the application's existing
 * PasswordEncoder bean (never persisted in plaintext, never logged), and
 * verified here - a client marking a handover "confirmed" locally has no
 * effect on this module's state.
 */
@Service
@RequiredArgsConstructor
public class HandoverServiceImpl implements HandoverService {

    @Autowired(required = false)
    private NotificationEventPublisher notificationEventPublisher;

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 30;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final VehicleHandoverRepository handoverRepository;
    private final NavigationTripRepository navigationTripRepository;
    private final NavigationRequestRepository navigationRequestRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    /**
     * Key material for deriving codes. Domain-separated from JWT signing by the HMAC message prefix, and never
     * exposed. A database leak alone does not reveal pending codes (only a bcrypt hash and a random nonce).
     */
    @Value("${security.jwt.secret:}")
    private String codeSecret;

    /**
     * Optional dedicated secret (variable HANDOVER_CODE_SECRET, at least 32 characters). When set it is used instead of
     * the JWT key, so compromising or rotating the JWT key no longer affects handover codes. Unset keeps the previous
     * behaviour. Either way the HMAC key is a purpose-specific subkey, never the raw secret.
     */
    @Value("${handover.code-secret:}")
    private String dedicatedCodeSecret;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public HandoverCodeResponse getOrCreateActiveCode(Long tripId) {
        return issueCode(tripId, false);
    }

    @Override
    @Transactional
    public HandoverCodeResponse renewCode(Long tripId) {
        return issueCode(tripId, true);
    }

    private HandoverCodeResponse issueCode(Long tripId, boolean forceNew) {

        GarageUserPrincipal principal = currentPrincipal();

        if (!principal.getRoles().contains(RoleCode.CUSTOMER.name())) {
            throw new ResourceNotFoundException("Trip not found : " + tripId);
        }

        Customer customer = customerRepository.findByMobileNumber(principal.getMobile())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found : " + tripId));

        // Row lock: concurrent requests for one trip queue here, so they cannot create competing active codes.
        NavigationTrip trip = navigationTripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found : " + tripId));

        NavigationRequest navigationRequest = navigationRequestRepository
                .findById(trip.getNavigationRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found : " + tripId));

        if (!navigationRequest.getCustomerId().equals(customer.getId())) {
            throw new ResourceNotFoundException("Trip not found : " + tripId);
        }

        if (trip.getTripType() == TripType.PICKUP
                && trip.getCurrentLeg() == TripLeg.CUSTOMER_TO_GARAGE) {
            throw new BusinessException(
                    "The vehicle handover was already confirmed - the pickup trip is on its way to the garage.");
        }

        if (trip.getArrivedAt() == null || trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new BusinessException(
                    "Handover is not available yet - your driver hasn't arrived.");
        }

        if (handoverRepository.existsByTripIdAndDirectionAndStatus(
                tripId, trip.getTripType(), HandoverStatus.VERIFIED)) {
            throw new BusinessException("The vehicle handover was already confirmed.");
        }

        LocalDateTime now = LocalDateTime.now();

        if (!forceNew) {
            VehicleHandover pending = handoverRepository
                    .findFirstByTripIdAndStatusOrderByCreatedAtDesc(tripId, HandoverStatus.PENDING)
                    .orElse(null);

            if (pending != null) {
                boolean live = pending.getExpiresAt().isAfter(now)
                        && pending.getFailedAttempts() < MAX_FAILED_ATTEMPTS;

                if (live && pending.getCodeNonce() != null) {
                    String derived = deriveCode(pending.getCodeNonce(), tripId, trip.getTripType());

                    // Only re-show a code that provably matches the stored hash. If the secret changed since it was
                    // issued, the derivation no longer matches and a fresh code is issued instead of a wrong one.
                    if (passwordEncoder.matches(derived, pending.getCodeHash())) {
                        return HandoverCodeResponse.builder()
                                .tripId(tripId)
                                .direction(trip.getTripType())
                                .code(derived)
                                .expiresAt(pending.getExpiresAt())
                                .newlyIssued(false)
                                .build();
                    }
                }
            }
        }

        // Replace: retire the old pending code (expired, exhausted, legacy, or explicitly renewed) before the new one exists.
        expireExistingActiveCode(tripId);

        String nonce = newNonce();
        String code = deriveCode(nonce, tripId, trip.getTripType());

        VehicleHandover handover = VehicleHandover.builder()
                .tripId(tripId)
                .direction(trip.getTripType())
                .codeHash(passwordEncoder.encode(code))
                .codeNonce(nonce)
                .status(HandoverStatus.PENDING)
                .expiresAt(now.plusMinutes(EXPIRY_MINUTES))
                .failedAttempts(0)
                .build();

        handover = handoverRepository.saveAndFlush(handover);

        // Never include the code itself in audit metadata - it is
        // security-sensitive material, same as a password.
        auditService.record(
                AuditEventType.OTP_GENERATED,
                "VehicleHandover",
                handover.getId(),
                navigationRequest.getGarageId(),
                java.util.Map.of("tripId", tripId, "direction", trip.getTripType(), "renewed", forceNew)
        );

        NotificationEvents.publish(notificationEventPublisher,
                NotificationEvent.of(NotificationEventType.HANDOVER_CODE_GENERATED,
                                navigationRequest.getGarageId(), tripId, navigationRequest.getJobCardId(), handover.getId())
                        .fact(NotificationFacts.CUSTOMER_ID, navigationRequest.getCustomerId())
                        .fact(NotificationFacts.DRIVER_USER_ID, trip.getDriverId())
                        .fact(NotificationFacts.TRIP_ID, tripId));

        return HandoverCodeResponse.builder()
                .tripId(tripId)
                .direction(trip.getTripType())
                .code(code)
                .expiresAt(handover.getExpiresAt())
                .newlyIssued(true)
                .build();
    }

    // noRollbackFor: a wrong code is reported by throwing, and the failed-attempt count (and an expiry/lock state change)
    // written just before must be committed. Without it the throw rolled the increment back, so the 5-attempt limit never
    // took effect (found by HandoverConcurrencyIT).
    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public HandoverStatusResponse verify(Long tripId, String code) {

        GarageUserPrincipal principal = currentPrincipal();

        Long driverId = principal.getId();

        NavigationTrip trip = navigationTripRepository.findByIdAndDriverId(tripId, driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found for driver."));

        NavigationRequest navigationRequest = navigationRequestRepository
                .findById(trip.getNavigationRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found for driver."));

        // Defense in depth: a driver's own garage must match the trip's
        // garage even though findByIdAndDriverId already scopes to this
        // driver's own assigned trips.
        if (principal.getGarageId() == null
                || !navigationRequest.getGarageId().equals(principal.getGarageId())) {

            throw new ResourceNotFoundException("Trip not found for driver.");
        }

        if (trip.getTripType() == TripType.PICKUP
                && trip.getCurrentLeg() == TripLeg.CUSTOMER_TO_GARAGE) {
            throw new BusinessException(
                    "The vehicle handover was already confirmed - complete the pickup trip at the garage.");
        }

        // Locked read: two concurrent verifications of one code cannot both succeed (single use).
        VehicleHandover handover = handoverRepository
                .lockPendingForTrip(tripId, HandoverStatus.PENDING)
                .stream().findFirst()
                .orElseThrow(() -> new BusinessException(
                        "No active confirmation code. Ask the customer to generate a new one."));

        if (!handover.getExpiresAt().isAfter(LocalDateTime.now())) {

            handover.setStatus(HandoverStatus.EXPIRED);
            handoverRepository.save(handover);

            throw new BusinessException(
                    "This confirmation code has expired. Ask the customer to generate a new one.");
        }

        if (handover.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {

            handover.setStatus(HandoverStatus.EXPIRED);
            handoverRepository.save(handover);

            throw new BusinessException(
                    "Too many incorrect attempts. Ask the customer to generate a new code.");
        }

        if (!passwordEncoder.matches(code, handover.getCodeHash())) {

            handover.setFailedAttempts(handover.getFailedAttempts() + 1);
            handoverRepository.save(handover);

            throw new BusinessException("Incorrect confirmation code.");
        }

        handover.setStatus(HandoverStatus.VERIFIED);
        handover.setVerifiedAt(LocalDateTime.now());
        handover.setVerifiedByDriverId(driverId);

        handoverRepository.save(handover);

        auditService.record(
                AuditEventType.OTP_VERIFIED,
                "VehicleHandover",
                handover.getId(),
                navigationRequest.getGarageId(),
                java.util.Map.of("tripId", tripId, "direction", handover.getDirection(), "verifiedByDriverId", driverId)
        );

        NotificationEvents.publish(notificationEventPublisher,
                NotificationEvent.of(NotificationEventType.HANDOVER_VERIFIED,
                                navigationRequest.getGarageId(), tripId, navigationRequest.getJobCardId(), handover.getId())
                        .fact(NotificationFacts.CUSTOMER_ID, navigationRequest.getCustomerId())
                        .fact(NotificationFacts.TRIP_ID, tripId));

        auditService.record(
                handover.getDirection() == TripType.DELIVERY
                        ? AuditEventType.VEHICLE_CUSTODY_RETURNED
                        : AuditEventType.VEHICLE_CUSTODY_TRANSFERRED_TO_GARAGE,
                "NavigationTrip",
                tripId,
                navigationRequest.getGarageId(),
                java.util.Map.of()
        );

        return toStatusResponse(handover);
    }

    @Override
    @Transactional(readOnly = true)
    public HandoverStatusResponse getStatus(Long tripId) {

        GarageUserPrincipal principal = currentPrincipal();

        if (principal.getGarageId() == null) {
            throw new ResourceNotFoundException("Trip not found : " + tripId);
        }

        NavigationTrip trip = navigationTripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found : " + tripId));

        NavigationRequest navigationRequest = navigationRequestRepository
                .findById(trip.getNavigationRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found : " + tripId));

        if (!navigationRequest.getGarageId().equals(principal.getGarageId())) {
            throw new ResourceNotFoundException("Trip not found : " + tripId);
        }

        VehicleHandover handover = handoverRepository
                .findFirstByTripIdOrderByCreatedAtDesc(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No handover has been issued for this trip yet."));

        return toStatusResponse(handover);
    }

    @Override
    @Transactional
    public void recordEvidenceViewed(Long tripId) {

        GarageUserPrincipal principal = currentPrincipal();

        if (!principal.getRoles().contains(RoleCode.CUSTOMER.name())) {
            throw new ResourceNotFoundException("Trip not found : " + tripId);
        }

        Customer customer = customerRepository.findByMobileNumber(principal.getMobile())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found : " + tripId));

        NavigationTrip trip = navigationTripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found : " + tripId));

        NavigationRequest navigationRequest = navigationRequestRepository
                .findById(trip.getNavigationRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found : " + tripId));

        if (!navigationRequest.getCustomerId().equals(customer.getId())) {
            throw new ResourceNotFoundException("Trip not found : " + tripId);
        }

        auditService.record(
                AuditEventType.CUSTOMER_VIEWED_EVIDENCE,
                "NavigationTrip",
                tripId,
                navigationRequest.getGarageId(),
                java.util.Map.of("direction", trip.getTripType())
        );
    }

    private void expireExistingActiveCode(Long tripId) {

        handoverRepository
                .findFirstByTripIdAndStatusOrderByCreatedAtDesc(tripId, HandoverStatus.PENDING)
                .ifPresent(existing -> {
                    existing.setStatus(HandoverStatus.EXPIRED);
                    handoverRepository.saveAndFlush(existing);
                });
    }

    private String newNonce() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Six digits derived as HMAC-SHA256(secret, "handover-code-v1|nonce|trip|direction"). The nonce comes from
     * SecureRandom, so the code is unpredictable without the server secret; the plaintext is recomputed on demand
     * and never stored or logged.
     */
    String deriveCode(String nonce, Long tripId, TripType direction) {

        String base = dedicatedCodeSecret != null && dedicatedCodeSecret.length() >= 32 ? dedicatedCodeSecret : codeSecret;

        if (base == null || base.length() < 16) {
            throw new IllegalStateException("Handover code secret is not configured.");
        }

        try {
            // Key separation: derive a purpose-specific subkey so the raw secret is never used directly as an HMAC key.
            Mac derive = Mac.getInstance("HmacSHA256");
            derive.init(new SecretKeySpec(base.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] subKey = derive.doFinal("garagest/handover-code/subkey/v1".getBytes(StandardCharsets.UTF_8));

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(subKey, "HmacSHA256"));
            byte[] h = mac.doFinal(("handover-code-v1|" + nonce + "|" + tripId + "|" + direction)
                    .getBytes(StandardCharsets.UTF_8));

            long v = 0;
            for (int i = 0; i < 8; i++) {
                v = (v << 8) | (h[i] & 0xffL);
            }
            long mod = (long) Math.pow(10, CODE_LENGTH);

            return String.format("%0" + CODE_LENGTH + "d", Long.remainderUnsigned(v, mod));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not derive handover code.", e);
        }
    }

    private GarageUserPrincipal currentPrincipal() {

        return (GarageUserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private HandoverStatusResponse toStatusResponse(VehicleHandover handover) {

        return HandoverStatusResponse.builder()
                .tripId(handover.getTripId())
                .direction(handover.getDirection())
                .status(handover.getStatus())
                .expiresAt(handover.getExpiresAt())
                .verifiedAt(handover.getVerifiedAt())
                .build();
    }
}
