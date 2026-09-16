package com.garageos.core.enums.booking;

/**
 * Booking is customer service-request intent ("I want this vehicle
 * serviced by this garage around this time") — deliberately separate from
 * {@link com.garageos.core.enums.JobCardStatus}, which only begins once the
 * garage has actually received/opened work on the vehicle. Do not conflate
 * the two lifecycles.
 */
public enum BookingStatus {

    REQUESTED,
    CONFIRMED,
    REJECTED,
    CANCELLED,
    COMPLETED
}
