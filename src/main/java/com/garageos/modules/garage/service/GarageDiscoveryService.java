package com.garageos.modules.garage.service;

import com.garageos.modules.garage.dto.response.NearbyGarageResponse;

import java.util.List;

public interface GarageDiscoveryService {

    /**
     * ACTIVE garages with usable coordinates within {@code radiusKm}
     * (configurable default when null) of the given point, nearest first.
     */
    List<NearbyGarageResponse> findNearby(double latitude, double longitude, Double radiusKm);
}
