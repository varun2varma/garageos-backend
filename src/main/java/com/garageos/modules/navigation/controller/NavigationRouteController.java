package com.garageos.modules.navigation.controller;

import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.navigation.dto.request.RouteRequest;
import com.garageos.modules.navigation.dto.response.RouteResponse;
import com.garageos.modules.navigation.service.NavigationRouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Route calculation for every screen that shows a road route (manager
 * pre-acceptance map, driver navigation, customer live journey, fleet
 * map) - not trip-scoped, so kept separate from NavigationTripController
 * (/api/v1/navigation/trips) rather than bolted onto it. No @PreAuthorize:
 * a route between two points carries no tenant/ownership data of its own
 * (SecurityConfig's default .anyRequest().authenticated() already applies),
 * matching every other read-only navigation endpoint's access level.
 */
@RestController
@RequestMapping("/api/v1/navigation")
@RequiredArgsConstructor
public class NavigationRouteController {

    private final NavigationRouteService navigationRouteService;

    @PostMapping("/routes")
    public ResponseEntity<?> getRoute(@Valid @RequestBody RouteRequest request) {

        return navigationRouteService.getRoute(request)
                .<ResponseEntity<?>>map(route ->
                        ApiResponseUtil.success("Route calculated successfully.", route))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
