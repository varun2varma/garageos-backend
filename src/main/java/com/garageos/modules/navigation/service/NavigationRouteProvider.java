package com.garageos.modules.navigation.service;

import com.garageos.modules.navigation.dto.response.RouteResponse;

import java.util.Optional;

/**
 * One route source (Google Routes, OSRM, ...) - mirrors the Flutter
 * client's own RouteProvider abstraction (core/maps/route_provider.dart)
 * on purpose, per the mission's "use the existing RouteResult semantics
 * where possible". Never throws for a routing failure (bad response,
 * timeout, no key configured) - returns Optional.empty() so
 * NavigationRouteServiceImpl can fall through to the next provider
 * without a try/catch at every call site, and never fabricates a route.
 */
public interface NavigationRouteProvider {

    Optional<RouteResponse> getRoute(
            double originLatitude,
            double originLongitude,
            double destinationLatitude,
            double destinationLongitude);
}
