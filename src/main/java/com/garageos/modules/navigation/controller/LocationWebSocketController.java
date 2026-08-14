package com.garageos.modules.navigation.controller;

import com.garageos.modules.navigation.dto.DriverLocationRequest;
import com.garageos.modules.navigation.service.DriverLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class LocationWebSocketController {

    private final DriverLocationService driverLocationService;

    @MessageMapping("/location/update")
    public void updateLocation(
            DriverLocationRequest request) {

        driverLocationService.processLocation(request);
    }
}