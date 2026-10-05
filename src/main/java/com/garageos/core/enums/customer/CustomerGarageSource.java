package com.garageos.core.enums.customer;

/** How a customer came to be related to a garage (see V66). */
public enum CustomerGarageSource {

    /** A job card was actually created at this garage - strongest, staff-verified event. */
    JOB_CARD,

    /** The garage confirmed a customer booking. */
    BOOKING_CONFIRMED,

    /**
     * Garage staff registered the customer (or onboarded an existing one by
     * mobile number). A real business association: the customer is that
     * garage's customer even before any job card or booking exists.
     */
    STAFF_REGISTERED;

    /**
     * Every source is a genuine association and makes the garage part of
     * the customer's "My Garages". Discovery/browsing never creates a row.
     */
    public boolean isServiceRelationship() {
        return true;
    }
}
