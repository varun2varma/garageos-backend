package com.garageos.modules.customer.dto.response.portal;

/**
 * The customer-facing "where is my vehicle in its journey" stage.
 *
 * This is deliberately NOT a persisted column anywhere - it is derived at
 * request time in {@link com.garageos.modules.customer.service.impl.CustomerVehicleJourneyServiceImpl}
 * from the real source-of-truth states (Booking, NavigationRequest,
 * NavigationTrip, JobCard, VehicleHandover), each of which keeps owning
 * its own lifecycle exactly as before. See that class for the full
 * mapping and the priority rule used when more than one domain state
 * exists at once.
 *
 * {@link #VEHICLE_PICKED_UP} is kept in this enum for API
 * forward-compatibility but is never currently returned by the
 * derivation service - see that method's own comment for why the
 * backend cannot currently distinguish "just picked up" from "still en
 * route to the garage" with the timestamps NavigationTrip stores today.
 */
public enum LiveVehicleJourneyStage {

    NO_ACTIVE_JOURNEY,

    BOOKING_CONFIRMED,

    DRIVER_ASSIGNED,

    DRIVER_EN_ROUTE,

    DRIVER_ARRIVED,

    /** Reserved - not currently derived. See class-level Javadoc. */
    VEHICLE_PICKED_UP,

    EN_ROUTE_TO_GARAGE,

    ARRIVED_AT_GARAGE,

    INSPECTION,

    ESTIMATE_PENDING,

    CUSTOMER_APPROVAL_REQUIRED,

    REPAIR_PENDING,

    REPAIR_IN_PROGRESS,

    QUALITY_CHECK,

    PAYMENT_PENDING,

    VEHICLE_READY,

    HANDOVER,

    COMPLETED
}
