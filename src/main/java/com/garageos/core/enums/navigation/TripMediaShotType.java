package com.garageos.core.enums.navigation;

/**
 * Which angle/aspect of the vehicle a pickup/delivery evidence photo
 * shows — orthogonal to {@link TripMediaStage} (stage is WHEN: before
 * pickup vs. delivery; shot type is WHAT). Mission Part I: configurable
 * evidence requirements, not a hardcoded photo count. Nullable on the
 * entity so pre-existing rows (captured before this field existed) remain
 * valid, just without this metadata.
 */
public enum TripMediaShotType {

    FRONT,

    REAR,

    LEFT,

    RIGHT,

    INTERIOR,

    ODOMETER,

    /** Existing damage, or anything not covered by the angles above. */
    OTHER
}
