package com.garageos.modules.navigation.service;

import com.garageos.modules.navigation.dto.request.RouteRequest;
import com.garageos.modules.navigation.dto.response.RouteResponse;

import java.util.Optional;

public interface NavigationRouteService {

    Optional<RouteResponse> getRoute(RouteRequest request);
}
