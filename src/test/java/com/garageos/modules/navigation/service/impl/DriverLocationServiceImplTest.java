package com.garageos.modules.navigation.service.impl;

import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.navigation.dto.DriverLocationRequest;
import com.garageos.modules.navigation.entity.DriverCurrentLocation;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.repository.DriverCurrentLocationRepository;
import com.garageos.modules.navigation.repository.DriverLocationHistoryRepository;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.repository.NavigationTripRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Covers the STOMP authorization gap traced for this task: `/app/location/
 * update` previously trusted whatever driverId the message body claimed,
 * with nothing tying it to who the WebSocket session actually
 * authenticated as. An authenticated CUSTOMER (or any other authenticated
 * user) could spoof a location update for someone else's trip by putting
 * that driver's id in the payload.
 */
@ExtendWith(MockitoExtension.class)
class DriverLocationServiceImplTest {

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

    private static final Long DRIVER_ID = 50L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long TRIP_ID = 700L;

    private DriverLocationRequest locationRequest(Long driverId) {
        DriverLocationRequest request = new DriverLocationRequest();
        request.setDriverId(driverId);
        request.setTripId(TRIP_ID);
        request.setLatitude(17.5);
        request.setLongitude(78.4);
        return request;
    }

    private NavigationTrip inProgressTrip() {
        return NavigationTrip.builder()
                .id(TRIP_ID).driverId(DRIVER_ID).status(TripStatus.IN_PROGRESS)
                .build();
    }

    @Test
    void authenticatedDriverUpdatingTheirOwnTrip_succeedsAndBroadcasts() {

        when(navigationTripRepository.findById(TRIP_ID)).thenReturn(Optional.of(inProgressTrip()));
        when(currentLocationRepository.findByTripId(TRIP_ID)).thenReturn(Optional.empty());
        when(currentLocationRepository.save(any(DriverCurrentLocation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service().processLocation(locationRequest(DRIVER_ID), DRIVER_ID);

        verify(messagingTemplate).convertAndSend(eq("/topic/trips/" + TRIP_ID + "/location"), any(DriverCurrentLocation.class));
    }

    @Test
    void unauthenticatedSession_isRejectedBeforeTouchingTheTrip() {

        assertThatThrownBy(() -> service().processLocation(locationRequest(DRIVER_ID), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("authenticated");

        verifyNoInteractions(navigationTripRepository, currentLocationRepository, messagingTemplate);
    }

    @Test
    void authenticatedAsADifferentUser_cannotReportLocationForAnotherDriver() {

        // The exact spoofing case this fixes: an authenticated session
        // (any role) sending a payload claiming to be DRIVER_ID.
        assertThatThrownBy(() -> service().processLocation(locationRequest(DRIVER_ID), OTHER_USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different driver");

        verifyNoInteractions(navigationTripRepository, currentLocationRepository, messagingTemplate);
    }

    @Test
    void resolveAuthenticatedDriverId_returnsTheGarageUserPrincipalId() {

        GarageUserPrincipal principal = new GarageUserPrincipal(
                DRIVER_ID, 10L, "driver1", "hash", "First", "Last", "d@test.io", "9999999999",
                false, UserStatus.ACTIVE, Set.of("DRIVER"), Set.of(), List.of());
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        assertThat(service().resolveAuthenticatedDriverId(authentication)).isEqualTo(DRIVER_ID);
    }

    @Test
    void resolveAuthenticatedDriverId_returnsNullForAnUnauthenticatedSession() {

        assertThat(service().resolveAuthenticatedDriverId(null)).isNull();
    }
}
