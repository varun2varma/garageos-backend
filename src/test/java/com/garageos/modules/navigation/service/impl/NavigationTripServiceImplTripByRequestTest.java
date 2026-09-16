package com.garageos.modules.navigation.service.impl;

import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.enums.navigation.TripLeg;
import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.core.enums.navigation.TripType;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.handover.repository.VehicleHandoverRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.navigation.dto.response.NavigationTripResponse;
import com.garageos.modules.navigation.entity.NavigationRequest;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.repository.NavigationTripMediaRepository;
import com.garageos.modules.navigation.repository.NavigationTripRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.*;

/**
 * Lets a customer resolve the actual trip for their Booking's
 * navigationRequestId (the only id the Booking response carries), without
 * a second, weaker lookup path.
 */
@ExtendWith(MockitoExtension.class)
class NavigationTripServiceImplTripByRequestTest {

    @Mock private NavigationRequestRepository navigationRequestRepository;
    @Mock private NavigationTripRepository navigationTripRepository;
    @Mock private NavigationTripMediaRepository navigationTripMediaRepository;
    @Mock private VehicleHandoverRepository vehicleHandoverRepository;
    @Mock private CustomerRepository customerRepository;

    // Injected so a trip's starting address can be the garage's real
    // address instead of its raw id.
    @Mock private GarageRepository garageRepository;
    @Mock private UserRepository userRepository;

    private NavigationTripServiceImpl service() {
        return new NavigationTripServiceImpl(
                navigationRequestRepository,
                navigationTripRepository,
                navigationTripMediaRepository,
                vehicleHandoverRepository,
                customerRepository,
                garageRepository,
                userRepository
        );
    }

    private static final Long REQUEST_ID = 300L;
    private static final Long TRIP_ID = 700L;
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

    private NavigationRequest navigationRequest() {
        return NavigationRequest.builder()
                .id(REQUEST_ID).customerId(CUSTOMER_ID).vehicleId(1L).garageId(GARAGE_ID)
                .build();
    }

    private NavigationTrip trip() {
        return NavigationTrip.builder()
                .id(TRIP_ID).navigationRequestId(REQUEST_ID).vehicleId(1L).driverId(DRIVER_ID)
                .tripType(TripType.PICKUP).currentLeg(TripLeg.GARAGE_TO_CUSTOMER).status(TripStatus.ASSIGNED)
                .build();
    }

    @Test
    void ownCustomer_canResolveTheirTrip() {

        authenticateAsCustomer("9000000000");
        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        when(customerRepository.findByMobileNumber("9000000000")).thenReturn(Optional.of(customer));
        when(navigationRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));
        when(navigationTripRepository.findFirstByNavigationRequestIdOrderByIdDesc(REQUEST_ID))
                .thenReturn(Optional.of(trip()));

        NavigationTripResponse response = service().getTripByRequest(REQUEST_ID);

        assertThat(response.getId()).isEqualTo(TRIP_ID);
    }

    @Test
    void anotherCustomer_isRejectedAsNotFound() {

        authenticateAsCustomer("9000000000");
        Customer other = new Customer();
        other.setId(999L);
        when(customerRepository.findByMobileNumber("9000000000")).thenReturn(Optional.of(other));
        when(navigationRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));
        when(navigationTripRepository.findFirstByNavigationRequestIdOrderByIdDesc(REQUEST_ID))
                .thenReturn(Optional.of(trip()));

        assertThatThrownBy(() -> service().getTripByRequest(REQUEST_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void noTripCreatedYet_isRejectedAsNotFound() {

        authenticateAsEmployee(GARAGE_ID);
        when(navigationRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));
        when(navigationTripRepository.findFirstByNavigationRequestIdOrderByIdDesc(REQUEST_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getTripByRequest(REQUEST_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void differentGarageEmployee_isRejectedAsNotFound() {

        authenticateAsEmployee(OTHER_GARAGE_ID);
        when(navigationRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));
        when(navigationTripRepository.findFirstByNavigationRequestIdOrderByIdDesc(REQUEST_ID))
                .thenReturn(Optional.of(trip()));

        assertThatThrownBy(() -> service().getTripByRequest(REQUEST_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sameGarageEmployee_canResolveTrip() {

        authenticateAsEmployee(GARAGE_ID);
        when(navigationRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));
        when(navigationTripRepository.findFirstByNavigationRequestIdOrderByIdDesc(REQUEST_ID))
                .thenReturn(Optional.of(trip()));

        NavigationTripResponse response = service().getTripByRequest(REQUEST_ID);

        assertThat(response.getId()).isEqualTo(TRIP_ID);
    }
}
