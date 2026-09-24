package com.garageos.modules.navigation.service.impl;

import com.garageos.modules.navigation.dto.request.RouteRequest;
import com.garageos.modules.navigation.dto.response.RouteResponse;
import com.garageos.modules.navigation.service.NavigationRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orchestrates the two route providers (mission Part Q/R): Google Routes
 * first when configured, OSRM as the fallback - never both in parallel
 * (routing performance rule), never a fabricated straight-line route when
 * both fail. This is the one place that decision is made; callers
 * (NavigationRouteController today) never see which provider answered
 * beyond RouteResponse.provider.
 */
@Service
@RequiredArgsConstructor
public class NavigationRouteServiceImpl implements NavigationRouteService {

    private final GoogleRoutesProvider googleRoutesProvider;
    private final OsrmRoutesProvider osrmRoutesProvider;

    @Override
    public Optional<RouteResponse> getRoute(RouteRequest request) {

        double originLat = request.getOrigin().getLatitude();
        double originLng = request.getOrigin().getLongitude();
        double destLat = request.getDestination().getLatitude();
        double destLng = request.getDestination().getLongitude();

        if (googleRoutesProvider.isConfigured()) {
            Optional<RouteResponse> googleResult =
                    googleRoutesProvider.getRoute(originLat, originLng, destLat, destLng);
            if (googleResult.isPresent()) {
                return googleResult;
            }
        }

        return osrmRoutesProvider.getRoute(originLat, originLng, destLat, destLng);
    }
}
