package com.garageos.modules.notification.event;

/** Keys of the small id-only fact map stored with an outbox event. */
public final class NotificationFacts {

    private NotificationFacts() {
    }

    public static final String ACTOR_USER_ID = "actorUserId";
    public static final String JOB_CARD_NUMBER = "jobCardNumber";
    public static final String CUSTOMER_ID = "customerId";
    public static final String DRIVER_USER_ID = "driverUserId";
    /** List of user ids captured at event time (e.g. before a QC reset). */
    public static final String TECHNICIAN_USER_IDS = "technicianUserIds";
    public static final String BOOKING_ID = "bookingId";
    public static final String TRIP_ID = "tripId";
}
