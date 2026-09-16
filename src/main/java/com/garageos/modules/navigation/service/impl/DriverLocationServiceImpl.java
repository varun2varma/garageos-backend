package com.garageos.modules.navigation.service.impl;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
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
import com.garageos.modules.navigation.service.DriverLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public void processLocation(DriverLocationRequest request) {

        validate(request);

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

        currentLocation.setDriverId(request.getDriverId());
        currentLocation.setTripId(request.getTripId());

        currentLocation.setLatitude(request.getLatitude());
        currentLocation.setLongitude(request.getLongitude());

        currentLocation.setSpeed(request.getSpeed());
        currentLocation.setHeading(request.getHeading());
        currentLocation.setAccuracy(request.getAccuracy());

        currentLocation.setLastUpdated(
                convertTimestamp(request.getTimestamp())
        );

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
     * may read its location - the same three-way viewer split
     * JobCardProjectionServiceImpl already uses for JobCard visibility.
     */
    private void authorizeViewer(NavigationTrip trip, NavigationRequest navigationRequest) {

        GarageUserPrincipal principal = (GarageUserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (principal.getRoles().contains(RoleCode.CUSTOMER.name())) {

            Customer customer = customerRepository.findByMobileNumber(principal.getMobile())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Trip not found : " + trip.getId()));

            if (!navigationRequest.getCustomerId().equals(customer.getId())) {
                throw new ResourceNotFoundException("Trip not found : " + trip.getId());
            }

            return;
        }

        boolean isAssignedDriver = trip.getDriverId() != null
                && trip.getDriverId().equals(principal.getId());

        boolean isSameGarageEmployee = principal.getGarageId() != null
                && principal.getGarageId().equals(navigationRequest.getGarageId());

        if (!isAssignedDriver && !isSameGarageEmployee) {
            throw new ResourceNotFoundException("Trip not found : " + trip.getId());
        }
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