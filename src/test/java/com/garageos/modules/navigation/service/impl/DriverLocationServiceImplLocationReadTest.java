package com.garageos.modules.navigation.service.impl;

import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.enums.navigation.TripLeg;
import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.core.enums.navigation.TripType;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.navigation.dto.response.TripLocationResponse;
import com.garageos.modules.navigation.entity.DriverCurrentLocation;
import com.garageos.modules.navigation.entity.NavigationRequest;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.repository.DriverCurrentLocationRepository;
import com.garageos.modules.navigation.repository.DriverLocationHistoryRepository;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.repository.NavigationTripRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Part 4: the smallest correct REST addition over the existing
 * WebSocket-only location stream. Authorization must match exactly the
 * three eligible viewers - own customer, assigned driver, same-garage
 * employee - and reject everyone else with not-found (never leaking that
 * a trip exists to an unauthorized caller).
 */
@ExtendWith(MockitoExtension.class)
class DriverLocationServiceImplLocationReadTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private DriverLocationHistoryRepository historyRepository;
    @Mock private DriverCurrentLocationRepository currentLocationRepository;
    @Mock private NavigationTripRepository navigationTripRepository;
    @Mock private NavigationRequestRepository navigationRequestRepository;
    @Mock private CustomerRepository customerRepository;

    private DriverLocationServiceImpl service() {
        return new DriverLocationServiceImpl(
                messagingTemplate,
                historyRepository,
                currentLocationRepository,
                navigationTripRepository,
                navigationRequestRepository,
                customerRepository
        );
    }

    private static final Long TRIP_ID = 700L;
    private static final Long NAV_REQUEST_ID = 300L;
    private static final Long CUSTOMER_ID = 100L;
    private static final Long DRIVER_ID = 50L;
    private static final Long GARAGE_ID = 10L;
    private static final Long OTHER_GARAGE_ID = 11L;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsCustomer(String mobile) {
        stubPrincipal(new GarageUserPrincipal(
                1L, null, "cust", "hash", "First", "Last", "c@test.io", mobile,
                false, UserStatus.ACTIVE, Set.of("CUSTOMER"), Set.of(), List.of()));
    }

    private void authenticateAsDriver(Long driverId, Long garageId) {
        stubPrincipal(new GarageUserPrincipal(
                driverId, garageId, "drv", "hash", "First", "Last", "d@test.io", "8888888888",
                false, UserStatus.ACTIVE, Set.of("DRIVER"), Set.of(), List.of()));
    }

    private void authenticateAsEmployee(Long garageId) {
        stubPrincipal(new GarageUserPrincipal(
                2L, garageId, "emp", "hash", "First", "Last", "e@test.io", "9999999999",
                false, UserStatus.ACTIVE, Set.of("MANAGER"), Set.of(), List.of()));
    }

    private void stubPrincipal(GarageUserPrincipal principal) {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private NavigationTrip trip() {
        return NavigationTrip.builder()
                .id(TRIP_ID).navigationRequestId(NAV_REQUEST_ID).vehicleId(1L).driverId(DRIVER_ID)
                .tripType(TripType.PICKUP).currentLeg(TripLeg.GARAGE_TO_CUSTOMER).status(TripStatus.IN_PROGRESS)
                .build();
    }

    private NavigationRequest navigationRequest() {
        return NavigationRequest.builder()
                .id(NAV_REQUEST_ID).customerId(CUSTOMER_ID).vehicleId(1L).garageId(GARAGE_ID)
                .build();
    }

    private DriverCurrentLocation location() {
        return DriverCurrentLocation.builder()
                .id(1L).driverId(DRIVER_ID).tripId(TRIP_ID)
                .latitude(12.9716).longitude(77.5946).lastUpdated(LocalDateTime.now())
                .build();
    }

    @Test
    void ownCustomer_canReadLocation() {

        authenticateAsCustomer("9000000000");
        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        when(customerRepository.findByMobileNumber("9000000000")).thenReturn(Optional.of(customer));
        when(navigationTripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip()));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));
        when(currentLocationRepository.findByTripId(TRIP_ID)).thenReturn(Optional.of(location()));

        TripLocationResponse response = service().getCurrentLocation(TRIP_ID);

        assertThat(response.getLatitude()).isEqualTo(12.9716);
        assertThat(response.getLongitude()).isEqualTo(77.5946);
    }

    @Test
    void anotherCustomer_isRejectedAsNotFound() {

        authenticateAsCustomer("9000000000");
        Customer other = new Customer();
        other.setId(999L);
        when(customerRepository.findByMobileNumber("9000000000")).thenReturn(Optional.of(other));
        when(navigationTripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip()));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));

        assertThatThrownBy(() -> service().getCurrentLocation(TRIP_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(currentLocationRepository);
    }

    @Test
    void assignedDriver_canReadLocation() {

        authenticateAsDriver(DRIVER_ID, GARAGE_ID);
        when(navigationTripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip()));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));
        when(currentLocationRepository.findByTripId(TRIP_ID)).thenReturn(Optional.of(location()));

        TripLocationResponse response = service().getCurrentLocation(TRIP_ID);

        assertThat(response.getTripId()).isEqualTo(TRIP_ID);
    }

    @Test
    void differentDriver_isRejectedAsNotFound() {

        authenticateAsDriver(999L, GARAGE_ID);
        when(navigationTripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip()));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));

        assertThatThrownBy(() -> service().getCurrentLocation(TRIP_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sameGarageEmployee_canReadLocation() {

        authenticateAsEmployee(GARAGE_ID);
        when(navigationTripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip()));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));
        when(currentLocationRepository.findByTripId(TRIP_ID)).thenReturn(Optional.of(location()));

        TripLocationResponse response = service().getCurrentLocation(TRIP_ID);

        assertThat(response.getTripId()).isEqualTo(TRIP_ID);
    }

    @Test
    void differentGarageEmployee_isRejectedAsNotFound() {

        authenticateAsEmployee(OTHER_GARAGE_ID);
        when(navigationTripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip()));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));

        assertThatThrownBy(() -> service().getCurrentLocation(TRIP_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(currentLocationRepository);
    }

    @Test
    void noLocationReportedYet_isRejectedAsNotFound() {

        authenticateAsEmployee(GARAGE_ID);
        when(navigationTripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip()));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));
        when(currentLocationRepository.findByTripId(TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getCurrentLocation(TRIP_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
