package com.garageos.modules.customer.dto.response.portal;

import lombok.Builder;
import lombok.Data;

/**
 * One vehicle's current, unified service journey, for
 * GET /api/v1/customer/vehicle-journeys/active.
 *
 * This is a read-only aggregation over Booking, NavigationRequest,
 * NavigationTrip and JobCard - {@link #journeyStage} and
 * {@link #journeyMessage} are derived fresh on every request in
 * {@link com.garageos.modules.customer.service.impl.CustomerVehicleJourneyServiceImpl},
 * never persisted, so they can never drift out of sync with the real
 * domain statuses they summarize.
 *
 * {@link #booking}, {@link #navigationTrip} and {@link #jobCard} are each
 * null when that domain has no bearing on the vehicle's current stage
 * (e.g. {@code navigationTrip} is null once the vehicle is being worked
 * on in the garage and no delivery trip has started yet).
 */
@Data
@Builder
public class CustomerLiveVehicleJourneySummary {

    private CustomerVehicleResponse vehicle;

    private LiveVehicleJourneyStage journeyStage;

    /** Short, customer-friendly copy for the active vehicle card. */
    private String journeyMessage;

    private VehicleJourneyBookingSummary booking;

    private VehicleJourneyTripSummary navigationTrip;

    private CustomerJobCardResponse jobCard;

    /** True when the customer needs to do something now (approve, pay). */
    private boolean actionRequired;

    private PrimaryActionType primaryAction;
}
