package com.garageos.modules.navigation.controller;

import com.garageos.modules.navigation.dto.request.CreateNavigationRequest;
import com.garageos.modules.navigation.dto.response.NavigationRequestResponse;
import com.garageos.modules.navigation.dto.response.NavigationTripResponse;
import com.garageos.modules.navigation.service.NavigationRequestService;
import com.garageos.modules.navigation.service.NavigationTripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/navigation/requests")
@RequiredArgsConstructor
public class NavigationRequestController {

    private final NavigationRequestService
            navigationRequestService;

    private final NavigationTripService
            navigationTripService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NavigationRequestResponse createRequest(
            @RequestParam Long customerId,
            @Valid @RequestBody
            CreateNavigationRequest request) {

        return navigationRequestService.createRequest(
                customerId,
                request
        );
    }

    @GetMapping("/{requestId}")
    public NavigationRequestResponse getRequest(
            @PathVariable Long requestId) {

        return navigationRequestService.getRequest(
                requestId
        );
    }

    @GetMapping("/customer/{customerId}")
    public List<NavigationRequestResponse>
    getCustomerRequests(
            @PathVariable Long customerId) {

        return navigationRequestService
                .getCustomerRequests(customerId);
    }

    @GetMapping("/garage/{garageId}")
    public List<NavigationRequestResponse>
    getGarageRequests(
            @PathVariable Long garageId) {

        return navigationRequestService
                .getGarageRequests(garageId);
    }

    /**
     * Lets a customer who only knows their Booking's navigationRequestId
     * (from BookingResponse.navigationRequestId) find the actual trip to
     * track pickup/delivery — authorized to that request's own customer,
     * its assigned driver, or garage-matched staff only.
     */
    @GetMapping("/{requestId}/trip")
    public NavigationTripResponse getTripForRequest(
            @PathVariable Long requestId) {

        return navigationTripService.getTripByRequest(requestId);
    }
}