package com.garageos.modules.navigation.service.impl;

import com.garageos.core.enums.navigation.HandoverStatus;
import com.garageos.core.enums.navigation.TripLeg;
import com.garageos.core.enums.navigation.TripMediaStage;
import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.core.enums.navigation.TripType;
import com.garageos.modules.handover.repository.VehicleHandoverRepository;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.entity.NavigationTripMedia;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.repository.NavigationTripMediaRepository;
import com.garageos.modules.navigation.repository.NavigationTripRepository;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Part 2 hardening: continuing a pickup trip past the customer, or
 * completing a delivery trip, must require an actually VERIFIED handover
 * for that exact trip and direction - not merely "a handover row exists."
 */
@ExtendWith(MockitoExtension.class)
class NavigationTripServiceImplHandoverGateTest {

    @Mock private NavigationRequestRepository navigationRequestRepository;
    @Mock private NavigationTripRepository navigationTripRepository;
    @Mock private NavigationTripMediaRepository navigationTripMediaRepository;
    @Mock private VehicleHandoverRepository vehicleHandoverRepository;

    @InjectMocks
    private NavigationTripServiceImpl service;

    private static final Long TRIP_ID = 700L;
    private static final Long DRIVER_ID = 50L;

    /**
     * Every driver-side transition now resolves the acting driver from the
     * authenticated principal, not from the driverId the caller supplied
     * (see NavigationTripServiceImpl.requireCallerIsDriver). These tests
     * exercise the trip's own legitimate driver, so they authenticate as
     * that driver; the authorization boundary itself is covered by
     * NavigationTripServiceImplDriverScopeTest.
     */
    @BeforeEach
    void authenticateAsAssignedDriver() {
        authenticateAsDriver(DRIVER_ID);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsDriver(Long driverId) {
        GarageUserPrincipal principal = new GarageUserPrincipal(
                driverId, 10L, "drv", "hash", "First", "Last", "d@test.io", "8888888888",
                false, UserStatus.ACTIVE, Set.of("DRIVER"), Set.of(), List.of());
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private NavigationTrip pickupTripOnOutboundLeg() {
        return NavigationTrip.builder()
                .id(TRIP_ID)
                .navigationRequestId(1L)
                .vehicleId(1L)
                .driverId(DRIVER_ID)
                .tripType(TripType.PICKUP)
                .currentLeg(TripLeg.GARAGE_TO_CUSTOMER)
                .status(TripStatus.IN_PROGRESS)
                .build();
    }

    private NavigationTrip pickupTripOnReturnLeg() {
        return NavigationTrip.builder()
                .id(TRIP_ID)
                .navigationRequestId(1L)
                .vehicleId(1L)
                .driverId(DRIVER_ID)
                .tripType(TripType.PICKUP)
                .currentLeg(TripLeg.CUSTOMER_TO_GARAGE)
                .status(TripStatus.IN_PROGRESS)
                .build();
    }

    private NavigationTrip deliveryTrip() {
        return NavigationTrip.builder()
                .id(TRIP_ID)
                .navigationRequestId(1L)
                .vehicleId(1L)
                .driverId(DRIVER_ID)
                .tripType(TripType.DELIVERY)
                .currentLeg(TripLeg.GARAGE_TO_CUSTOMER)
                .status(TripStatus.IN_PROGRESS)
                .build();
    }

    private void stubBeforePickupMediaPresent() {
        when(navigationTripMediaRepository.findByTripIdAndMediaStageOrderByCapturedAtAsc(TRIP_ID, TripMediaStage.BEFORE_PICKUP))
                .thenReturn(List.of(mock(NavigationTripMedia.class)));
    }

    private void stubDeliveryMediaPresent() {
        when(navigationTripMediaRepository.findByTripIdAndMediaStageOrderByCapturedAtAsc(TRIP_ID, TripMediaStage.DELIVERY))
                .thenReturn(List.of(mock(NavigationTripMedia.class)));
    }

    // ---- continueTrip (PICKUP gate) ----

    @Test
    void continueTrip_withoutVerifiedPickupHandover_isRejected_tripStateUnchanged() {

        NavigationTrip trip = pickupTripOnOutboundLeg();
        when(navigationTripRepository.findByIdAndDriverId(TRIP_ID, DRIVER_ID)).thenReturn(Optional.of(trip));
        stubBeforePickupMediaPresent();
        when(vehicleHandoverRepository.existsByTripIdAndDirectionAndStatus(TRIP_ID, TripType.PICKUP, HandoverStatus.VERIFIED))
                .thenReturn(false);

        assertThatThrownBy(() -> service.continueTrip(TRIP_ID, DRIVER_ID))
                .isInstanceOf(IllegalStateException.class);

        assertThat(trip.getCurrentLeg()).isEqualTo(TripLeg.GARAGE_TO_CUSTOMER);
        verify(navigationTripRepository, never()).save(any());
    }

    @Test
    void continueTrip_withVerifiedPickupHandover_succeeds() {

        NavigationTrip trip = pickupTripOnOutboundLeg();
        when(navigationTripRepository.findByIdAndDriverId(TRIP_ID, DRIVER_ID)).thenReturn(Optional.of(trip));
        stubBeforePickupMediaPresent();
        when(vehicleHandoverRepository.existsByTripIdAndDirectionAndStatus(TRIP_ID, TripType.PICKUP, HandoverStatus.VERIFIED))
                .thenReturn(true);
        when(navigationTripRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.continueTrip(TRIP_ID, DRIVER_ID);

        assertThat(trip.getCurrentLeg()).isEqualTo(TripLeg.CUSTOMER_TO_GARAGE);
    }

    @Test
    void continueTrip_deliveryHandoverVerified_doesNotAuthorizePickupContinuation() {

        // A DELIVERY-direction VERIFIED handover exists for this trip id,
        // but NOT a PICKUP one - must still fail (direction must match).
        NavigationTrip trip = pickupTripOnOutboundLeg();
        when(navigationTripRepository.findByIdAndDriverId(TRIP_ID, DRIVER_ID)).thenReturn(Optional.of(trip));
        stubBeforePickupMediaPresent();
        when(vehicleHandoverRepository.existsByTripIdAndDirectionAndStatus(TRIP_ID, TripType.PICKUP, HandoverStatus.VERIFIED))
                .thenReturn(false);

        assertThatThrownBy(() -> service.continueTrip(TRIP_ID, DRIVER_ID))
                .isInstanceOf(IllegalStateException.class);

        verify(vehicleHandoverRepository, never())
                .existsByTripIdAndDirectionAndStatus(eq(TRIP_ID), eq(TripType.DELIVERY), any());
    }

    // ---- completeTrip (DELIVERY gate) ----

    @Test
    void completeTrip_delivery_withoutVerifiedHandover_isRejected_statusUnchanged() {

        NavigationTrip trip = deliveryTrip();
        when(navigationTripRepository.findByIdAndDriverId(TRIP_ID, DRIVER_ID)).thenReturn(Optional.of(trip));
        stubDeliveryMediaPresent();
        when(vehicleHandoverRepository.existsByTripIdAndDirectionAndStatus(TRIP_ID, TripType.DELIVERY, HandoverStatus.VERIFIED))
                .thenReturn(false);

        assertThatThrownBy(() -> service.completeTrip(TRIP_ID, DRIVER_ID))
                .isInstanceOf(IllegalStateException.class);

        assertThat(trip.getStatus()).isEqualTo(TripStatus.IN_PROGRESS);
        verify(navigationTripRepository, never()).save(any());
    }

    @Test
    void completeTrip_delivery_withVerifiedHandover_succeeds() {

        NavigationTrip trip = deliveryTrip();
        when(navigationTripRepository.findByIdAndDriverId(TRIP_ID, DRIVER_ID)).thenReturn(Optional.of(trip));
        stubDeliveryMediaPresent();
        when(vehicleHandoverRepository.existsByTripIdAndDirectionAndStatus(TRIP_ID, TripType.DELIVERY, HandoverStatus.VERIFIED))
                .thenReturn(true);
        when(navigationTripRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.completeTrip(TRIP_ID, DRIVER_ID);

        assertThat(trip.getStatus()).isEqualTo(TripStatus.COMPLETED);
    }

    // ---- completeTrip (PICKUP gate, return-leg) ----

    @Test
    void completeTrip_pickup_withoutVerifiedHandover_isRejected() {

        NavigationTrip trip = pickupTripOnReturnLeg();
        when(navigationTripRepository.findByIdAndDriverId(TRIP_ID, DRIVER_ID)).thenReturn(Optional.of(trip));
        stubBeforePickupMediaPresent();
        when(vehicleHandoverRepository.existsByTripIdAndDirectionAndStatus(TRIP_ID, TripType.PICKUP, HandoverStatus.VERIFIED))
                .thenReturn(false);

        assertThatThrownBy(() -> service.completeTrip(TRIP_ID, DRIVER_ID))
                .isInstanceOf(IllegalStateException.class);

        assertThat(trip.getStatus()).isEqualTo(TripStatus.IN_PROGRESS);
        verify(navigationTripRepository, never()).save(any());
    }

    @Test
    void completeTrip_pickup_withVerifiedHandover_succeeds() {

        NavigationTrip trip = pickupTripOnReturnLeg();
        when(navigationTripRepository.findByIdAndDriverId(TRIP_ID, DRIVER_ID)).thenReturn(Optional.of(trip));
        stubBeforePickupMediaPresent();
        when(vehicleHandoverRepository.existsByTripIdAndDirectionAndStatus(TRIP_ID, TripType.PICKUP, HandoverStatus.VERIFIED))
                .thenReturn(true);
        when(navigationTripRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.completeTrip(TRIP_ID, DRIVER_ID);

        assertThat(trip.getStatus()).isEqualTo(TripStatus.COMPLETED);
    }
}
