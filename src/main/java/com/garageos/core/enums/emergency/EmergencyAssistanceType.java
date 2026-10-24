package com.garageos.core.enums.emergency;

/**
 * What kind of help the customer asks for. This is a label the garage reads, not a promise: a garage is never shown
 * as offering a service the system has no data for, and no external towing or roadside provider is called.
 */
public enum EmergencyAssistanceType {
    BREAKDOWN,
    FLAT_TYRE,
    BATTERY_JUMP_START,
    FUEL_DELIVERY,
    TOWING,
    ACCIDENT,
    LOCKOUT,
    OTHER
}
