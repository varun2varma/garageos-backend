package com.garageos.modules.navigation.controller;

import com.garageos.modules.navigation.dto.request.CreateNavigationRequest;
import com.garageos.modules.navigation.dto.response.NavigationRequestResponse;
import com.garageos.modules.navigation.service.NavigationRequestService;
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
}