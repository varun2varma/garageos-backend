package com.garageos.modules.navigation.security;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.navigation.entity.NavigationRequest;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.repository.NavigationTripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Single home for "may this caller see this navigation trip" - previously
 * duplicated, near-identically, in NavigationTripServiceImpl.authorizeViewer
 * and DriverLocationServiceImpl.authorizeViewer, and needed again by
 * NavigationTripMediaServiceImpl and the STOMP subscribe-time interceptor
 * (see TripLocationTopicInterceptor). Extracted so a future fix to this
 * rule only has to be made once. Behavior matches the two call sites this
 * replaces exactly: the trip's own customer, its assigned driver, or
 * garage-matched operational staff - anyone else gets a not-found (never a
 * 403), so probing a trip id cannot reveal that it exists in another
 * garage/customer's data.
 */
@Component
@RequiredArgsConstructor
public class NavigationTripAccessGuard {

    private final NavigationTripRepository navigationTripRepository;
    private final NavigationRequestRepository navigationRequestRepository;
    private final CustomerRepository customerRepository;

    /**
     * Loads the trip and its owning request and authorizes {@code
     * principal} to view them in one call - the shape the STOMP
     * interceptor needs, since it has neither loaded already.
     */
    public NavigationTrip authorizeViewer(GarageUserPrincipal principal, Long tripId) {

        NavigationTrip trip = navigationTripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found : " + tripId));

        NavigationRequest navigationRequest = trip.getNavigationRequestId() == null
                ? null
                : navigationRequestRepository.findById(trip.getNavigationRequestId()).orElse(null);

        if (navigationRequest == null) {
            // Cannot verify ownership without the owning request - fail
            // closed rather than silently allowing access.
            throw new ResourceNotFoundException("Trip not found : " + tripId);
        }

        authorizeViewer(principal, trip, navigationRequest);

        return trip;
    }

    public void authorizeViewer(
            Authentication authentication,
            NavigationTrip trip,
            NavigationRequest navigationRequest) {

        if (!(authentication.getPrincipal() instanceof GarageUserPrincipal principal)) {
            throw new ResourceNotFoundException("Trip not found : " + trip.getId());
        }

        authorizeViewer(principal, trip, navigationRequest);
    }

    public void authorizeViewer(
            GarageUserPrincipal principal,
            NavigationTrip trip,
            NavigationRequest navigationRequest) {

        if (principal.getRoles().contains(RoleCode.CUSTOMER.name())) {

            Customer customer = customerRepository.findByMobileNumber(principal.getMobile())
                    .orElseThrow(() -> new ResourceNotFoundException("Trip not found : " + trip.getId()));

            if (!navigationRequest.getCustomerId().equals(customer.getId())) {
                throw new ResourceNotFoundException("Trip not found : " + trip.getId());
            }

            return;
        }

        boolean isAssignedDriver = trip.getDriverId() != null
                && trip.getDriverId().equals(principal.getId());

        boolean isSameGarageEmployee = principal.getGarageId() != null
                && principal.getGarageId().equals(navigationRequest.getGarageId());

        if (!isAssignedDriver && !isSameGarageEmployee) {
            throw new ResourceNotFoundException("Trip not found : " + trip.getId());
        }
    }
}
