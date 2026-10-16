package com.garageos.core.enums.navigation;

public enum TripMediaStage {

    BEFORE_PICKUP,

    DELIVERY;

    /**
     * Default customer-visibility policy applied when evidence of this stage is uploaded. It is only the
     * initial value of the persisted per-photo flag; reports read the flag, never the stage alone.
     */
    public boolean defaultCustomerVisible() {
        return this == BEFORE_PICKUP || this == DELIVERY;
    }
}
