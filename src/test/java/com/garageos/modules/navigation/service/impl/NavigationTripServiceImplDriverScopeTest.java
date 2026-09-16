package com.garageos.modules.navigation.service.impl;

import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.enums.navigation.TripLeg;
import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.core.enums.navigation.TripType;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.handover.repository.VehicleHandoverRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.repository.NavigationTripMediaRepository;
import com.garageos.modules.navigation.repository.NavigationTripRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Driver-scope authorization for NavigationTrip.
 *
 * Every driver-side endpoint takes driverId as a plain path variable or
 * request parameter. Before this suite existed, the service validated only
 * that the trip belonged to that driverId - never that the caller actually
 * was that driver - so any authenticated user could read another driver's
 * queue and drive another driver's trip through its whole lifecycle.
 */
@ExtendWith(MockitoExtension.class)
class NavigationTripServiceImplDriverScopeTest {

    @Mock private NavigationRequestRepository navigationRequestRepository;
    @Mock private NavigationTripRepository navigationTripRepository;
    @Mock private NavigationTripMediaRepository navigationTripMediaRepository;
    @Mock private VehicleHandoverRepository vehicleHandoverRepository;

    @InjectMocks
    private NavigationTripServiceImpl service;

    private static final Long TRIP_ID = 700L;
    private static final Long DRIVER_ID = 50L;
    private static final Long OTHER_DRIVER_ID = 51L;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId, String role) {
        GarageUserPrincipal principal = new GarageUserPrincipal(
                userId, 10L, "user", "hash", "First", "Last", "u@test.io", "8888888888",
                false, UserStatus.ACTIVE, Set.of(role), Set.of(), List.of());
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private NavigationTrip assignedTrip() {
        return NavigationTrip.builder()
                .id(TRIP_ID)
                .navigationRequestId(1L)
                .vehicleId(1L)
                .driverId(DRIVER_ID)
                .tripType(TripType.PICKUP)
                .currentLeg(TripLeg.GARAGE_TO_CUSTOMER)
                .status(TripStatus.ASSIGNED)
                .build();
    }

    // ---- queue reads ----

    @Test
    void getDriverTrips_otherDriversQueue_isRejected_andNeverQueried() {

        authenticateAs(OTHER_DRIVER_ID, "DRIVER");

        assertThatThrownBy(() -> service.getDriverTrips(DRIVER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(navigationTripRepository, never())
                .findByDriverIdAndStatusIn(anyLong(), anyList());
    }

    @Test
    void getDriverTrips_ownQueue_isReturned() {

        authenticateAs(DRIVER_ID, "DRIVER");
        when(navigationTripRepository.findByDriverIdAndStatusIn(eq(DRIVER_ID), anyList()))
                .thenReturn(List.of(assignedTrip()));

        assertThat(service.getDriverTrips(DRIVER_ID)).hasSize(1);
    }

    @Test
    void getDriverTripHistory_otherDriversHistory_isRejected_andNeverQueried() {

        authenticateAs(OTHER_DRIVER_ID, "DRIVER");

        assertThatThrownBy(() -> service.getDriverTripHistory(DRIVER_ID, 25))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(navigationTripRepository, never())
                .findByDriverIdAndStatusInOrderByIdDesc(anyLong(), anyList(), any(Pageable.class));
    }

    @Test
    void getDriverTripHistory_limitIsCappedAndAtLeastOne() {

        authenticateAs(DRIVER_ID, "DRIVER");
        when(navigationTripRepository.findByDriverIdAndStatusInOrderByIdDesc(
                eq(DRIVER_ID), anyList(), any(Pageable.class)))
                .thenReturn(List.of());

        service.getDriverTripHistory(DRIVER_ID, 5000);
        service.getDriverTripHistory(DRIVER_ID, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(navigationTripRepository, times(2))
                .findByDriverIdAndStatusInOrderByIdDesc(eq(DRIVER_ID), anyList(), captor.capture());

        assertThat(captor.getAllValues().get(0).getPageSize()).isEqualTo(100);
        assertThat(captor.getAllValues().get(1).getPageSize()).isEqualTo(1);
    }

    // ---- lifecycle transitions ----

    @Test
    void acceptTrip_asAnotherDriver_isRejected_andTripNeverLoadedOrSaved() {

        authenticateAs(OTHER_DRIVER_ID, "DRIVER");

        assertThatThrownBy(() -> service.acceptTrip(TRIP_ID, DRIVER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(navigationTripRepository, never()).findByIdAndDriverId(anyLong(), anyLong());
        verify(navigationTripRepository, never()).save(any());
    }

    @Test
    void startTrip_asAnotherDriver_isRejected() {

        authenticateAs(OTHER_DRIVER_ID, "DRIVER");

        assertThatThrownBy(() -> service.startTrip(TRIP_ID, DRIVER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(navigationTripRepository, never()).save(any());
    }

    @Test
    void completeTrip_asAnotherDriver_isRejected() {

        authenticateAs(OTHER_DRIVER_ID, "DRIVER");

        assertThatThrownBy(() -> service.completeTrip(TRIP_ID, DRIVER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(navigationTripRepository, never()).save(any());
    }

    /**
     * A garage Manager is not the driver either. Operational staff read
     * trips through the trip/location endpoints, which carry their own
     * garage-scoped checks - they must not be able to drive a trip's
     * lifecycle on a driver's behalf through this path.
     */
    @Test
    void acceptTrip_asManager_isRejected() {

        authenticateAs(OTHER_DRIVER_ID, "MANAGER");

        assertThatThrownBy(() -> service.acceptTrip(TRIP_ID, DRIVER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void acceptTrip_asAssignedDriver_succeeds() {

        authenticateAs(DRIVER_ID, "DRIVER");
        NavigationTrip trip = assignedTrip();
        when(navigationTripRepository.findByIdAndDriverId(TRIP_ID, DRIVER_ID))
                .thenReturn(Optional.of(trip));
        when(navigationTripRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.acceptTrip(TRIP_ID, DRIVER_ID).getStatus())
                .isEqualTo(TripStatus.ACCEPTED);
    }
}
