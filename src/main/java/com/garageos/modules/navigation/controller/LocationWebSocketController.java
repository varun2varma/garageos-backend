package com.garageos.modules.navigation.controller;

import com.garageos.modules.navigation.dto.DriverLocationRequest;
import com.garageos.modules.navigation.service.DriverLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class LocationWebSocketController {

    private final DriverLocationService driverLocationService;

    /**
     * Root-cause fix (STOMP authorization gap): this previously trusted
     * whatever driverId the message body claimed, with no check that the
     * WebSocket session actually belongs to that driver — an
     * authenticated CUSTOMER (or any other authenticated user) could send
     * a location update for someone else's trip simply by putting their
     * driverId in the payload. [principal] is populated by Spring's
     * WebSocket handshake handler from the same SecurityContext
     * JwtAuthenticationFilter already sets for the underlying HTTP
     * handshake (the STOMP endpoint requires authentication like every
     * other endpoint per SecurityConfig - "/ws" is not in the permitAll
     * list), so this is authenticating against the real signed-in user,
     * not the message body.
     */
    @MessageMapping("/location/update")
    public void updateLocation(
            DriverLocationRequest request,
            Principal principal) {

        Long authenticatedUserId = principal instanceof Authentication authentication
                ? driverLocationService.resolveAuthenticatedDriverId(authentication)
                : null;

        driverLocationService.processLocation(request, authenticatedUserId);
    }
}