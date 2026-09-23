package com.garageos.modules.navigation.controller;

import com.garageos.modules.navigation.dto.request.CreateNavigationTripRequest;
import com.garageos.modules.navigation.dto.response.FleetTripResponse;
import com.garageos.modules.navigation.dto.response.NavigationTripResponse;
import com.garageos.modules.navigation.dto.response.TripLocationResponse;
import com.garageos.modules.navigation.service.DriverLocationService;
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

    private final DriverLocationService
            driverLocationService;


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

    /**
     * Authorized read of the trip's last reported location - the REST
     * counterpart to the existing WebSocket-only broadcast at
     * /topic/trips/{tripId}/location, for the customer/driver/manager/
     * advisor/owner live-trip experience.
     */
    @GetMapping("/{tripId}/location")
    public TripLocationResponse getLocation(
            @PathVariable Long tripId) {

        return driverLocationService.getCurrentLocation(
                tripId
        );
    }


    /**
     * MANAGER fleet map (Mission Part O) - every active trip for the
     * caller's own garage, with driver identity and last-known GPS
     * position. Garage-scoping is enforced in
     * NavigationTripServiceImpl.requireOperationalStaffOfGarage, the same
     * check assignDriver already uses.
     */
    @GetMapping("/garage/{garageId}/fleet")
    public List<FleetTripResponse> getGarageFleet(
            @PathVariable Long garageId) {

        return navigationTripService.getGarageFleet(garageId);
    }


    /*
     * DRIVER
     */

    /**
     * The calling driver's own open trips. driverId must be the caller's
     * own id - enforced in NavigationTripServiceImpl, not here, so every
     * driver-scoped path shares one check.
     */
    @GetMapping("/driver/{driverId}")
    public List<NavigationTripResponse>
    getDriverTrips(
            @PathVariable Long driverId) {

        return navigationTripService
                .getDriverTrips(driverId);
    }


    /**
     * The calling driver's completed/cancelled trips, newest first.
     */
    @GetMapping("/driver/{driverId}/history")
    public List<NavigationTripResponse>
    getDriverTripHistory(
            @PathVariable Long driverId,
            @RequestParam(defaultValue = "25") int limit) {

        return navigationTripService
                .getDriverTripHistory(driverId, limit);
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