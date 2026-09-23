package com.garageos.modules.navigation.service;

import com.garageos.modules.navigation.dto.request.CreateNavigationTripRequest;
import com.garageos.modules.navigation.dto.response.FleetTripResponse;
import com.garageos.modules.navigation.dto.response.NavigationTripResponse;

import java.util.List;

public interface NavigationTripService {

    NavigationTripResponse assignDriver(
            CreateNavigationTripRequest request
    );

    NavigationTripResponse getTrip(
            Long tripId
    );

    /**
     * Resolves the (most recent) trip created for a NavigationRequest —
     * lets a customer who only knows their Booking's navigationRequestId
     * find the actual trip to track, without inventing a second lookup
     * path. Authorized the same way as DriverLocationService.getCurrentLocation:
     * the request's own customer, its assigned driver, or garage-matched
     * operational staff only.
     */
    NavigationTripResponse getTripByRequest(
            Long navigationRequestId
    );

    /**
     * The calling driver's open work queue (ASSIGNED / ACCEPTED /
     * IN_PROGRESS). {@code driverId} is only honoured when it is the
     * caller's own id - never trusted as an arbitrary lookup key.
     */
    List<NavigationTripResponse> getDriverTrips(
            Long driverId
    );

    /**
     * The calling driver's finished trips (COMPLETED / CANCELLED),
     * newest first, capped at {@code limit} (max 100).
     */
    List<NavigationTripResponse> getDriverTripHistory(
            Long driverId,
            int limit
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

    /**
     * Manager fleet map (Mission Part O) - every ASSIGNED/ACCEPTED/
     * IN_PROGRESS trip for the caller's own garage, with driver identity
     * and last-known GPS position. Caller must be operational staff of
     * {@code garageId} (same check assignDriver uses) - never another
     * garage's fleet.
     */
    List<FleetTripResponse> getGarageFleet(Long garageId);
}