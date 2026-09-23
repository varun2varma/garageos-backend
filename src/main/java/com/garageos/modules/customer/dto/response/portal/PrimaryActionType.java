package com.garageos.modules.customer.dto.response.portal;

/**
 * What the customer's primary action button on an active vehicle journey
 * card should do, machine-readable so Flutter owns the exact label/copy
 * and route rather than the backend baking UI text into the response.
 */
public enum PrimaryActionType {

    NONE,

    TRACK_VEHICLE,

    VIEW_PICKUP,

    VIEW_PROGRESS,

    REVIEW_ESTIMATE,

    VIEW_REPAIR_PROGRESS,

    PAY_NOW,

    VIEW_VEHICLE,

    COMPLETE_HANDOVER
}
