package com.garageos.modules.navigation.service;

import com.garageos.modules.navigation.dto.request.CreateNavigationTripRequest;
import com.garageos.modules.navigation.dto.response.NavigationTripResponse;

import java.util.List;

public interface NavigationTripService {

    NavigationTripResponse assignDriver(
            CreateNavigationTripRequest request
    );

    NavigationTripResponse getTrip(
            Long tripId
    );

    List<NavigationTripResponse> getDriverTrips(
            Long driverId
    );

    NavigationTripResponse acceptTrip(
            Long tripId,
            Long driverId
    );

    NavigationTripResponse startTrip(
            Long tripId,
            Long driverId
    );

    NavigationTripResponse arriveAtDestination(
            Long tripId,
            Long driverId
    );

    NavigationTripResponse continueTrip(
            Long tripId,
            Long driverId
    );

    NavigationTripResponse completeTrip(
            Long tripId,
            Long driverId
    );
}