package com.garageos.modules.garage.service.impl;

import com.garageos.core.exception.BusinessException;
import com.garageos.modules.garage.dto.response.NearbyGarageResponse;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.garage.service.GarageDiscoveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Server-side nearby search: a cheap bounding-box filter in the database
 * (plain numeric lat/lng, no PostGIS) followed by an exact Haversine
 * distance filter/sort in memory.
 */
@Service
public class GarageDiscoveryServiceImpl implements GarageDiscoveryService {

    private static final double EARTH_RADIUS_KM = 6371.0088;
    private static final double KM_PER_DEGREE_LAT = 111.32;

    private final GarageRepository garageRepository;
    private final double defaultRadiusKm;
    private final double maxRadiusKm;
    private final int maxResults;

    public GarageDiscoveryServiceImpl(
            GarageRepository garageRepository,
            @Value("${garage.discovery.default-radius-km:40}") double defaultRadiusKm,
            @Value("${garage.discovery.max-radius-km:100}") double maxRadiusKm,
            @Value("${garage.discovery.max-results:50}") int maxResults) {

        this.garageRepository = garageRepository;
        this.defaultRadiusKm = defaultRadiusKm;
        this.maxRadiusKm = maxRadiusKm;
        this.maxResults = maxResults;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbyGarageResponse> findNearby(
            double latitude, double longitude, Double radiusKm) {

        if (Double.isNaN(latitude) || Double.isNaN(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new BusinessException("A valid latitude and longitude are required.");
        }

        double radius = radiusKm == null ? defaultRadiusKm : radiusKm;

        if (radius <= 0 || radius > maxRadiusKm) {
            throw new BusinessException(
                    "Radius must be greater than 0 and at most " + maxRadiusKm + " km.");
        }

        double latDelta = radius / KM_PER_DEGREE_LAT;
        double cosLat = Math.max(Math.cos(Math.toRadians(latitude)), 0.01);
        double lonDelta = radius / (KM_PER_DEGREE_LAT * cosLat);

        List<Garage> candidates = garageRepository.findActiveWithinBox(
                BigDecimal.valueOf(latitude - latDelta),
                BigDecimal.valueOf(latitude + latDelta),
                BigDecimal.valueOf(longitude - lonDelta),
                BigDecimal.valueOf(longitude + lonDelta));

        return candidates.stream()
                .map(g -> new Object[]{g, haversineKm(latitude, longitude,
                        g.getLatitude().doubleValue(), g.getLongitude().doubleValue())})
                .filter(pair -> (double) pair[1] <= radius)
                .sorted(Comparator.comparingDouble(pair -> (double) pair[1]))
                .limit(maxResults)
                .map(pair -> toResponse((Garage) pair[0], (double) pair[1]))
                .toList();
    }

    static double haversineKm(double lat1, double lon1, double lat2, double lon2) {

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private NearbyGarageResponse toResponse(Garage g, double distanceKm) {

        return NearbyGarageResponse.builder()
                .id(g.getId())
                .garageName(g.getGarageName())
                .address(g.getAddress())
                .landmark(g.getLandmark())
                .city(g.getCity())
                .state(g.getState())
                .pincode(g.getPincode())
                .latitude(g.getLatitude())
                .longitude(g.getLongitude())
                .hasLogo(g.getLogoStorageKey() != null)
                .distanceKm(Math.round(distanceKm * 10.0) / 10.0)
                .build();
    }
}
