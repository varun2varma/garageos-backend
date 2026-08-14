package com.garageos.modules.navigation.service.impl;

import com.garageos.modules.navigation.dto.DriverLocationRequest;
import com.garageos.modules.navigation.entity.DriverCurrentLocation;
import com.garageos.modules.navigation.entity.DriverLocationHistory;
import com.garageos.modules.navigation.repository.DriverCurrentLocationRepository;
import com.garageos.modules.navigation.repository.DriverLocationHistoryRepository;
import com.garageos.modules.navigation.service.DriverLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    @Override
    @Transactional
    public void processLocation(DriverLocationRequest request) {

        validate(request);

        log.debug(
                "Processing location: driverId={}, tripId={}, lat={}, lon={}",
                request.getDriverId(),
                request.getTripId(),
                request.getLatitude(),
                request.getLongitude()
        );

        DriverCurrentLocation currentLocation =
                saveCurrentLocation(request);

        saveLocationHistory(request);

        broadcastLocation(currentLocation);
    }

    private DriverCurrentLocation saveCurrentLocation(
            DriverLocationRequest request) {

        DriverCurrentLocation currentLocation =
                currentLocationRepository
                        .findByDriverId(request.getDriverId())
                        .orElseGet(DriverCurrentLocation::new);

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