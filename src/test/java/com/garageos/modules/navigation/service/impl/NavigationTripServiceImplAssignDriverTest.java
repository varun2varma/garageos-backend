package com.garageos.modules.navigation.service.impl;

import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.enums.navigation.NavigationRequestStatus;
import com.garageos.core.enums.navigation.NavigationRequestType;
import com.garageos.core.exception.BusinessException;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.handover.repository.VehicleHandoverRepository;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.navigation.dto.request.CreateNavigationTripRequest;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers the duplicate-assignment root cause traced against Flutter's
 * "Navigation request is not available for assignment" error: both the
 * ordinary (non-concurrent) rejection path, and the V42 unique-index
 * backstop for the race two concurrent submissions can hit under READ
 * COMMITTED.
 */
@ExtendWith(MockitoExtension.class)
class NavigationTripServiceImplAssignDriverTest {

    @Mock private NavigationRequestRepository navigationRequestRepository;
    @Mock private NavigationTripRepository navigationTripRepository;
    @Mock private NavigationTripMediaRepository navigationTripMediaRepository;
    @Mock private VehicleHandoverRepository vehicleHandoverRepository;
    @Mock private CustomerRepository customerRepository;
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
    private static final Long DRIVER_ID = 50L;
    private static final Long GARAGE_ID = 10L;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsEmployee(Long garageId) {
        GarageUserPrincipal principal = new GarageUserPrincipal(
                2L, garageId, "emp", "hash", "First", "Last", "e@test.io", "9999999999",
                false, UserStatus.ACTIVE, Set.of("MANAGER"), Set.of(), List.of());
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private NavigationRequest requestedNavigationRequest() {
        return NavigationRequest.builder()
                .id(REQUEST_ID).customerId(100L).vehicleId(1L).garageId(GARAGE_ID)
                .requestType(NavigationRequestType.PICKUP)
                .status(NavigationRequestStatus.REQUESTED)
                .pickupAddress("123 Main St")
                .build();
    }

    private User driverInGarage() {
        User driver = new User();
        driver.setId(DRIVER_ID);
        driver.setGarageId(GARAGE_ID);
        return driver;
    }

    private CreateNavigationTripRequest assignRequest() {
        CreateNavigationTripRequest request = new CreateNavigationTripRequest();
        request.setNavigationRequestId(REQUEST_ID);
        request.setDriverId(DRIVER_ID);
        return request;
    }

    @Test
    void firstAssignment_succeedsAndMarksRequestAssigned() {

        authenticateAsEmployee(GARAGE_ID);
        NavigationRequest navigationRequest = requestedNavigationRequest();
        when(navigationRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(navigationRequest));
        when(userRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driverInGarage()));
        when(navigationTripRepository.save(any(NavigationTrip.class)))
                .thenAnswer(invocation -> {
                    NavigationTrip trip = invocation.getArgument(0);
                    trip.setId(700L);
                    return trip;
                });

        NavigationTripResponse response = service().assignDriver(assignRequest());

        assertThat(response.getId()).isEqualTo(700L);
        assertThat(navigationRequest.getStatus()).isEqualTo(NavigationRequestStatus.ASSIGNED);
        verify(navigationRequestRepository).save(navigationRequest);
    }

    @Test
    void requestAlreadyAssigned_isRejectedWithoutCreatingASecondTrip() {

        authenticateAsEmployee(GARAGE_ID);
        NavigationRequest navigationRequest = requestedNavigationRequest();
        navigationRequest.setStatus(NavigationRequestStatus.ASSIGNED);
        when(navigationRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(navigationRequest));

        assertThatThrownBy(() -> service().assignDriver(assignRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not available for assignment")
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo("NAVIGATION_ALREADY_ASSIGNED"));

        verify(navigationTripRepository, never()).save(any());
    }

    @Test
    void concurrentDuplicateAssignment_uniqueConstraintBackstopSurfacesAsControlledError() {

        // Simulates the race: this transaction's own read still saw
        // REQUESTED (so the code-level check above passes), but another
        // concurrent transaction committed its trip first, so the
        // database's unique index (V42) rejects this save.
        authenticateAsEmployee(GARAGE_ID);
        NavigationRequest navigationRequest = requestedNavigationRequest();
        when(navigationRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(navigationRequest));
        when(userRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driverInGarage()));
        when(navigationTripRepository.save(any(NavigationTrip.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThatThrownBy(() -> service().assignDriver(assignRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not available for assignment")
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo("NAVIGATION_ALREADY_ASSIGNED"));

        // The request must not be left silently ASSIGNED when the trip
        // itself was never actually persisted by this transaction.
        verify(navigationRequestRepository, never()).save(any());
    }
}
