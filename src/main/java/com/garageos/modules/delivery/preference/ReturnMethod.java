package com.garageos.modules.delivery.preference;

/** How the customer wants the finished vehicle returned. */
public enum ReturnMethod {
    /** Customer collects the vehicle at the garage; no driver trip is created. */
    COLLECT_AT_GARAGE,
    /** A garage driver delivers the vehicle to the customer's chosen address. */
    DRIVER_DELIVERY
}
