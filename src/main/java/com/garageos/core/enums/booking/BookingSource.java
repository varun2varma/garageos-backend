package com.garageos.core.enums.booking;

/**
 * Mission backlog #6 — phone-call booking. A booking created by the
 * customer themselves through the app vs. one an employee creates on
 * their behalf from a phone call (BookingController's /phone endpoint).
 */
public enum BookingSource {

    CUSTOMER_APP,

    PHONE
}
