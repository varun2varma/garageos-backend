package com.garageos.core.enums.emergency;

/** Lifecycle of an Emergency Assist request. REQUESTED and ACCEPTED are the "active" states. */
public enum EmergencyRequestStatus {
    REQUESTED,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    EXPIRED,
    COMPLETED;

    public boolean isActive() {
        return this == REQUESTED || this == ACCEPTED;
    }
}
