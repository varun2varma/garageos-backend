package com.garageos.modules.navigation.controller;

import com.garageos.modules.navigation.dto.request.CreateNavigationTripRequest;
import com.garageos.modules.navigation.dto.response.NavigationTripResponse;
import com.garageos.modules.navigation.service.NavigationTripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/navigation/trips")
@RequiredArgsConstructor
public class NavigationTripController {

    private final NavigationTripService
            navigationTripService;


    /*
     * MANAGER
     *
     * Assign navigation request to driver.
     */

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NavigationTripResponse assignDriver(
            @Valid @RequestBody
            CreateNavigationTripRequest request) {

        return navigationTripService.assignDriver(
                request
        );
    }


    @GetMapping("/{tripId}")
    public NavigationTripResponse getTrip(
            @PathVariable Long tripId) {

        return navigationTripService.getTrip(
                tripId
        );
    }


    /*
     * DRIVER
     */

    @GetMapping("/driver/{driverId}")
    public List<NavigationTripResponse>
    getDriverTrips(
            @PathVariable Long driverId) {

        return navigationTripService
                .getDriverTrips(driverId);
    }


    @PostMapping("/{tripId}/accept")
    public NavigationTripResponse acceptTrip(
            @PathVariable Long tripId,
            @RequestParam Long driverId) {

        return navigationTripService.acceptTrip(
                tripId,
                driverId
        );
    }


    @PostMapping("/{tripId}/start")
    public NavigationTripResponse startTrip(
            @PathVariable Long tripId,
            @RequestParam Long driverId) {

        return navigationTripService.startTrip(
                tripId,
                driverId
        );
    }


    @PostMapping("/{tripId}/arrive")
    public NavigationTripResponse arrive(
            @PathVariable Long tripId,
            @RequestParam Long driverId) {

        return navigationTripService.arriveAtDestination(
                tripId,
                driverId
        );
    }


    @PostMapping("/{tripId}/continue")
    public NavigationTripResponse continueTrip(
            @PathVariable Long tripId,
            @RequestParam Long driverId) {

        return navigationTripService.continueTrip(
                tripId,
                driverId
        );
    }


    @PostMapping("/{tripId}/complete")
    public NavigationTripResponse completeTrip(
            @PathVariable Long tripId,
            @RequestParam Long driverId) {

        return navigationTripService.completeTrip(
                tripId,
                driverId
        );
    }
}