package com.garageos.modules.handover.service.impl;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.enums.navigation.HandoverStatus;
import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
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

import java.security.SecureRandom;
import java.time.LocalDateTime;

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

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 10;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final VehicleHandoverRepository handoverRepository;
    private final NavigationTripRepository navigationTripRepository;
    private final NavigationRequestRepository navigationRequestRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public HandoverCodeResponse getOrCreateActiveCode(Long tripId) {

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

        if (trip.getArrivedAt() == null || trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new BusinessException(
                    "Handover is not available yet - your driver hasn't arrived.");
        }

        expireExistingActiveCode(tripId);

        String code = generateCode();

        VehicleHandover handover = VehicleHandover.builder()
                .tripId(tripId)
                .direction(trip.getTripType())
                .codeHash(passwordEncoder.encode(code))
                .status(HandoverStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES))
                .failedAttempts(0)
                .build();

        handoverRepository.save(handover);

        return HandoverCodeResponse.builder()
                .tripId(tripId)
                .direction(trip.getTripType())
                .code(code)
                .expiresAt(handover.getExpiresAt())
                .build();
    }

    @Override
    @Transactional
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

        VehicleHandover handover = handoverRepository
                .findFirstByTripIdAndStatusOrderByCreatedAtDesc(tripId, HandoverStatus.PENDING)
                .orElseThrow(() -> new BusinessException(
                        "No active confirmation code. Ask the customer to generate a new one."));

        if (handover.getExpiresAt().isBefore(LocalDateTime.now())) {

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

    private void expireExistingActiveCode(Long tripId) {

        handoverRepository
                .findFirstByTripIdAndStatusOrderByCreatedAtDesc(tripId, HandoverStatus.PENDING)
                .ifPresent(existing -> {
                    existing.setStatus(HandoverStatus.EXPIRED);
                    handoverRepository.save(existing);
                });
    }

    private String generateCode() {

        int max = (int) Math.pow(10, CODE_LENGTH);

        int value = secureRandom.nextInt(max);

        return String.format("%0" + CODE_LENGTH + "d", value);
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
