package com.garageos.modules.navigation.service.impl;

import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.navigation.dto.DriverLocationRequest;
import com.garageos.modules.navigation.dto.response.TripLocationResponse;
import com.garageos.modules.navigation.entity.DriverCurrentLocation;
import com.garageos.modules.navigation.entity.DriverLocationHistory;
import com.garageos.modules.navigation.entity.NavigationRequest;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.repository.DriverCurrentLocationRepository;
import com.garageos.modules.navigation.repository.DriverLocationHistoryRepository;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.repository.NavigationTripRepository;
import com.garageos.modules.navigation.security.NavigationTripAccessGuard;
import com.garageos.modules.navigation.service.DriverLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverLocationServiceImpl
        implements DriverLocationService {

    private final SimpMessagingTemplate messagingTemplate;

    private final DriverLocationHistoryRepository historyRepository;

    private final DriverCurrentLocationRepository currentLocationRepository;

    private final NavigationTripRepository navigationTripRepository;

    private final NavigationRequestRepository navigationRequestRepository;

    private final NavigationTripAccessGuard accessGuard;

    @Override
    public Long resolveAuthenticatedDriverId(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof GarageUserPrincipal principal)) {

            return null;
        }

        return principal.getId();
    }

    @Override
    @Transactional
    public void processLocation(DriverLocationRequest request, Long authenticatedUserId) {

        validate(request);

        // Root-cause fix: previously the driverId in the message body was
        // trusted outright - the only check was that it matched the
        // trip's assigned driver, which any authenticated user could
        // still satisfy by simply putting that driver's id in the
        // payload. This ties the update to who the WebSocket session
        // actually authenticated as.
        if (authenticatedUserId == null) {

            throw new IllegalStateException(
                    "Location updates require an authenticated session."
            );
        }

        if (!authenticatedUserId.equals(request.getDriverId())) {

            throw new IllegalStateException(
                    "Cannot report location for a different driver."
            );
        }

        NavigationTrip trip =
                validateActiveTrip(request);

        log.debug(
                "Processing location: driverId={}, tripId={}, lat={}, lon={}",
                request.getDriverId(),
                request.getTripId(),
                request.getLatitude(),
                request.getLongitude()
        );

        DriverCurrentLocation currentLocation =
                saveCurrentLocation(request, trip);

        saveLocationHistory(request);

        broadcastLocation(currentLocation);
    }

    private NavigationTrip validateActiveTrip(
            DriverLocationRequest request) {

        NavigationTrip trip =
                navigationTripRepository
                        .findById(
                                request.getTripId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Navigation trip not found: "
                                                + request.getTripId()
                                )
                        );


        if (!request.getDriverId()
                .equals(trip.getDriverId())) {

            throw new IllegalArgumentException(
                    "Driver is not assigned to this trip"
            );
        }


        if (trip.getStatus()
                != TripStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "Location updates are allowed only for active trips"
            );
        }


        return trip;
    }

    private DriverCurrentLocation saveCurrentLocation(
            DriverLocationRequest request,
            NavigationTrip trip) {

        DriverCurrentLocation currentLocation =
                currentLocationRepository
                        .findByTripId(
                                request.getTripId()
                        )
                        .orElseGet(
                                DriverCurrentLocation::new
                        );

        LocalDateTime incomingTimestamp =
                convertTimestamp(request.getTimestamp());

        // Root-cause fix (MASTER_E2E_COVERAGE.md Known Issue #3): STOMP's
        // default multi-threaded inbound dispatch gives no per-trip
        // ordering guarantee, so a late-arriving-but-older update could
        // otherwise overwrite a newer one here and make the driver's
        // marker visibly jump backward on the map. The point is still
        // recorded in history by the caller either way - only this
        // "current position" projection ignores an update that is older
        // than what it already has.
        boolean isStaleUpdate =
                currentLocation.getId() != null
                        && currentLocation.getLastUpdated() != null
                        && incomingTimestamp.isBefore(
                                currentLocation.getLastUpdated()
                        );

        if (isStaleUpdate) {

            log.debug(
                    "Dropping stale location update: tripId={}, incoming={}, current={}",
                    request.getTripId(),
                    incomingTimestamp,
                    currentLocation.getLastUpdated()
            );

            return currentLocation;
        }

        currentLocation.setDriverId(request.getDriverId());
        currentLocation.setTripId(request.getTripId());

        currentLocation.setLatitude(request.getLatitude());
        currentLocation.setLongitude(request.getLongitude());

        currentLocation.setSpeed(request.getSpeed());
        currentLocation.setHeading(request.getHeading());
        currentLocation.setAccuracy(request.getAccuracy());

        currentLocation.setLastUpdated(incomingTimestamp);

        return currentLocationRepository.save(currentLocation);
    }

    private void saveLocationHistory(
            DriverLocationRequest request) {

        DriverLocationHistory history =
                DriverLocationHistory.builder()
                        .driverId(request.getDriverId())
                        .tripId(request.getTripId())
                        .latitude(request.getLatitude())
                        .longitude(request.getLongitude())
                        .speed(request.getSpeed())
                        .heading(request.getHeading())
                        .accuracy(request.getAccuracy())
                        .locationTime(
                                convertTimestamp(request.getTimestamp())
                        )
                        .build();

        historyRepository.save(history);
    }

    private void broadcastLocation(
            DriverCurrentLocation currentLocation) {

        String destination =
                "/topic/trips/"
                        + currentLocation.getTripId()
                        + "/location";

        messagingTemplate.convertAndSend(
                destination,
                currentLocation
        );

        log.debug(
                "Location broadcasted: driverId={}, tripId={}",
                currentLocation.getDriverId(),
                currentLocation.getTripId()
        );
    }

    private LocalDateTime convertTimestamp(Long timestamp) {

        if (timestamp == null) {
            return LocalDateTime.now();
        }

        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneOffset.UTC
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TripLocationResponse getCurrentLocation(Long tripId) {

        NavigationTrip trip = navigationTripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found : " + tripId));

        NavigationRequest navigationRequest = navigationRequestRepository
                .findById(trip.getNavigationRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found : " + tripId));

        authorizeViewer(trip, navigationRequest);

        DriverCurrentLocation location = currentLocationRepository.findByTripId(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No location has been reported for this trip yet."));

        return TripLocationResponse.builder()
                .tripId(tripId)
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .speed(location.getSpeed())
                .heading(location.getHeading())
                .accuracy(location.getAccuracy())
                .lastUpdated(location.getLastUpdated())
                .build();
    }

    /**
     * Never trust a client-supplied tripId alone: only the trip's own
     * customer, its assigned driver, or garage-matched operational staff
     * may read its location. Delegates to NavigationTripAccessGuard, the
     * single shared home for this rule (also used by
     * NavigationTripServiceImpl, NavigationTripMediaServiceImpl, and the
     * STOMP subscribe-time interceptor).
     */
    private void authorizeViewer(NavigationTrip trip, NavigationRequest navigationRequest) {

        GarageUserPrincipal principal = (GarageUserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        accessGuard.authorizeViewer(principal, trip, navigationRequest);
    }

    private void validate(DriverLocationRequest request) {

        if (request.getDriverId() == null) {
            throw new IllegalArgumentException(
                    "Driver ID is required"
            );
        }

        if (request.getTripId() == null) {
            throw new IllegalArgumentException(
                    "Trip ID is required"
            );
        }

        if (request.getLatitude() == null
                || request.getLatitude() < -90
                || request.getLatitude() > 90) {

            throw new IllegalArgumentException(
                    "Invalid latitude"
            );
        }

        if (request.getLongitude() == null
                || request.getLongitude() < -180
                || request.getLongitude() > 180) {

            throw new IllegalArgumentException(
                    "Invalid longitude"
            );
        }
    }
}