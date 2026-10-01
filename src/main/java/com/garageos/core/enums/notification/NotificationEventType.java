package com.garageos.core.enums.notification;

import static com.garageos.core.enums.notification.NotificationCategory.*;
import static com.garageos.core.enums.notification.NotificationPriority.*;

/**
 * Closed set of business events that can produce a notification. Each value
 * only exists where a real business transition exists in the services; see
 * the call sites of NotificationEventPublisher.
 */
public enum NotificationEventType {

    BOOKING_REQUESTED(BOOKING, NORMAL, "BOOKING"),
    BOOKING_CREATED_BY_STAFF(BOOKING, LOW, "BOOKING"),
    BOOKING_CONFIRMED(BOOKING, NORMAL, "BOOKING"),
    BOOKING_REJECTED(BOOKING, NORMAL, "BOOKING"),
    BOOKING_CANCELLED(BOOKING, NORMAL, "BOOKING"),

    PICKUP_DRIVER_ASSIGNED(TRIP, HIGH, "TRIP"),
    TRIP_ACCEPTED(TRIP, NORMAL, "TRIP"),
    TRIP_STARTED(TRIP, NORMAL, "TRIP"),
    DRIVER_ARRIVED(TRIP, HIGH, "TRIP"),
    HANDOVER_CODE_GENERATED(TRIP, NORMAL, "TRIP"),
    HANDOVER_VERIFIED(TRIP, NORMAL, "TRIP"),
    TRIP_COMPLETED(TRIP, NORMAL, "TRIP"),

    JOB_CARD_CREATED(JOB_CARD, NORMAL, "JOB_CARD"),

    INSPECTION_STARTED(INSPECTION, LOW, "JOB_CARD"),
    INSPECTION_COMPLETED(INSPECTION, NORMAL, "JOB_CARD"),

    ESTIMATE_READY_FOR_APPROVAL(ESTIMATE, HIGH, "ESTIMATE"),
    ESTIMATE_APPROVED(ESTIMATE, HIGH, "ESTIMATE"),
    ESTIMATE_REJECTED(ESTIMATE, HIGH, "ESTIMATE"),

    REPAIR_TASK_ASSIGNED(ASSIGNMENT, HIGH, "REPAIR_TASK"),
    REPAIR_TASK_STARTED(REPAIR, LOW, "REPAIR_TASK"),
    REPAIR_TASK_COMPLETED(REPAIR, LOW, "REPAIR_TASK"),

    JOB_REPAIR_STARTED(REPAIR, NORMAL, "JOB_CARD"),
    JOB_REPAIR_COMPLETED(REPAIR, NORMAL, "JOB_CARD"),

    QUALITY_CHECK_STARTED(QUALITY_CHECK, LOW, "JOB_CARD"),
    QUALITY_CHECK_PASSED(QUALITY_CHECK, NORMAL, "JOB_CARD"),
    QUALITY_CHECK_FAILED(QUALITY_CHECK, HIGH, "JOB_CARD"),

    INVOICE_GENERATED(INVOICE, HIGH, "INVOICE"),
    INVOICE_ACCEPTED(INVOICE, NORMAL, "INVOICE"),
    PAYMENT_RECEIVED(PAYMENT, NORMAL, "INVOICE"),

    VEHICLE_READY_FOR_DELIVERY(DELIVERY, HIGH, "JOB_CARD"),
    VEHICLE_DELIVERED(DELIVERY, NORMAL, "JOB_CARD"),
    JOB_CLOSED(JOB_CARD, LOW, "JOB_CARD");

    private final NotificationCategory category;
    private final NotificationPriority priority;
    private final String entityType;

    NotificationEventType(NotificationCategory category,
                          NotificationPriority priority,
                          String entityType) {
        this.category = category;
        this.priority = priority;
        this.entityType = entityType;
    }

    public NotificationCategory getCategory() {
        return category;
    }

    public NotificationPriority getPriority() {
        return priority;
    }

    /** Default deep-link entity type (BOOKING, TRIP, JOB_CARD, REPAIR_TASK, ESTIMATE, INVOICE). */
    public String getEntityType() {
        return entityType;
    }
}
