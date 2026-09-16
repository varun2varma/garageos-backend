package com.garageos.modules.navigation.service;

import com.garageos.modules.navigation.dto.DriverLocationRequest;
import com.garageos.modules.navigation.dto.response.TripLocationResponse;
import org.springframework.security.core.Authentication;

public interface DriverLocationService {

    /**
     * [authenticatedUserId] is the id of the user the WebSocket session
     * actually authenticated as (null if the session somehow reached here
     * unauthenticated) - processLocation rejects the update unless it
     * matches request.getDriverId(), so a location update can never be
     * attributed to a driver other than whoever is actually connected.
     */
    void processLocation(DriverLocationRequest request, Long authenticatedUserId);

    /**
     * Resolves the numeric user id backing an authenticated WebSocket
     * session's Authentication - the same GarageUserPrincipal shape every
     * REST controller already authorizes against, so this reuses that
     * identity rather than introducing a second one for WebSocket.
     */
    Long resolveAuthenticatedDriverId(Authentication authentication);

    /**
     * Authorized read of a trip's last known location: the trip's own
     * customer, its assigned driver, or garage-matched operational staff
     * only. Throws ResourceNotFoundException for anyone else, and for a
     * trip with no location reported yet.
     */
    TripLocationResponse getCurrentLocation(Long tripId);
}