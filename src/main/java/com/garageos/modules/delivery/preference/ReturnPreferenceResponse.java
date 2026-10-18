package com.garageos.modules.delivery.preference;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ReturnPreferenceResponse {

    private Long jobCardId;
    /** Lets the customer app open the existing live vehicle journey / tracking for this vehicle. */
    private Long vehicleId;
    /** Null until the customer has chosen. */
    private ReturnMethod method;
    private String deliveryAddress;
    private BigDecimal deliveryLatitude;
    private BigDecimal deliveryLongitude;
    private LocalDateTime selectedAt;

    /** Requested delivery time (the DELIVERY request's scheduledAt); null when none was asked for. Not the actual handover time. */
    private LocalDateTime requestedDeliveryAt;

    /** The linked DELIVERY navigation request, when the choice is driver delivery. */
    private Long navigationRequestId;
    private String navigationRequestStatus;
    private Long tripId;
    private String tripStatus;

    /** True once a driver is assigned / the trip started: the choice can no longer be changed by the customer. */
    private boolean locked;
    /**
     * Derived: NOT_READY, WAITING_FOR_CUSTOMER_PREFERENCE, COLLECTION_SELECTED, DELIVERY_REQUESTED, DRIVER_ASSIGNED,
     * DRIVER_ACCEPTED, IN_TRANSIT, DRIVER_ARRIVED, HANDED_OVER. Managers see the waiting state from this.
     */
    private String deliveryState;

    /** True when the customer can currently submit or change the choice. */
    private boolean canChoose;
}
