package com.garageos.modules.navigation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * POST /api/v1/navigation/routes - the one route-calculation entry point
 * every caller (manager pre-acceptance map, driver navigation, customer
 * live journey, fleet map) shares. Origin/destination only; the caller
 * never supplies a provider or a key - which provider answers, and how,
 * is entirely a backend decision (NavigationRouteServiceImpl).
 */
@Getter
@Setter
public class RouteRequest {

    @NotNull(message = "Origin is required.")
    @Valid
    private LatLngRequest origin;

    @NotNull(message = "Destination is required.")
    @Valid
    private LatLngRequest destination;
}
