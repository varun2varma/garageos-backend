package com.garageos.modules.navigation.service;

import com.garageos.modules.navigation.dto.DriverLocationRequest;
import com.garageos.modules.navigation.dto.response.TripLocationResponse;

public interface DriverLocationService {

    void processLocation(DriverLocationRequest request);

    /**
     * Authorized read of a trip's last known location: the trip's own
     * customer, its assigned driver, or garage-matched operational staff
     * only. Throws ResourceNotFoundException for anyone else, and for a
     * trip with no location reported yet.
     */
    TripLocationResponse getCurrentLocation(Long tripId);
}