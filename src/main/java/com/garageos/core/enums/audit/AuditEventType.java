package com.garageos.core.enums.audit;

/**
 * Every lifecycle event this backend records an audit trail for (Mission's
 * audit requirement). Not every event name the mission listed maps to a
 * real, distinct backend transition: DRIVER_DECLINED has no mechanism
 * anywhere in this codebase (a driver can only accept, never decline -
 * inventing a decline flow is a new product capability, out of scope for
 * an audit-trail pass); ARRIVED_AT_GARAGE has no separate transition from
 * PICKUP_COMPLETED (the return leg goes straight from RETURN_TRIP_STARTED
 * to completion); DELIVERY_STARTED/DELIVERY_EVIDENCE_CAPTURED/
 * DELIVERY_OTP_VERIFIED are the same transitions as their pickup
 * counterparts (PICKUP_STARTED/EVIDENCE_CAPTURED/OTP_VERIFIED), so are
 * recorded as one event type with a "direction"/"stage" metadata field
 * rather than doubling the enum for the same code path.
 */
public enum AuditEventType {

    BOOKING_CREATED,
    BOOKING_CONFIRMED,
    BOOKING_REJECTED,
    BOOKING_CANCELLED,

    DRIVER_ASSIGNED,
    DRIVER_ACCEPTED,

    TRIP_STARTED,
    DRIVER_ARRIVED,

    EVIDENCE_CAPTURED,
    CUSTOMER_VIEWED_EVIDENCE,

    OTP_GENERATED,
    OTP_VERIFIED,

    VEHICLE_CUSTODY_TRANSFERRED_TO_GARAGE,
    VEHICLE_CUSTODY_RETURNED,

    RETURN_TRIP_STARTED,

    PICKUP_COMPLETED,
    DELIVERY_COMPLETED,

    /** The one-time, operator-controlled grant of the platform administrator role (no actor: it runs at startup). */
    PLATFORM_ADMIN_GRANTED,
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,

    /** A platform administrator corrected the classification snapshot of an approved estimate line (no financial change). */
    ESTIMATE_LINE_CLASSIFICATION_CORRECTED,

    /** An Owner authorized credit on an unpaid invoice (financial authorization; payment status unchanged). */
    CREDIT_AUTHORIZED,
    /** An Owner revoked an active credit authorization. */
    CREDIT_REVOKED,
    /** A repayment was recorded against an invoice that has an active credit authorization. */
    CREDIT_REPAYMENT,
    /** The invoice became fully paid and its credit authorization was closed. */
    CREDIT_SETTLED,

    /** A platform administrator activated or deactivated a garage (reason recorded in the metadata). */
    GARAGE_STATUS_CHANGED,
    /** A platform administrator's garage status change was refused (for example failed re-authentication). */
    GARAGE_STATUS_CHANGE_REFUSED,
    /** A platform administrator queried the platform audit trail. */
    PLATFORM_AUDIT_VIEWED,
    /** A garage owner changed the garage colour palette (previous and new values in the metadata). */
    GARAGE_BRANDING_UPDATED,
    /** Garage staff changed a vehicle's RC review status (previous and new status in the metadata). */
    VEHICLE_RC_STATUS_CHANGED,
    BANK_ACCOUNT_REGISTERED,
    BANK_ACCOUNT_RETIRED,
    /** A bank-account change was refused because the owner failed step-up re-authentication. */
    BANK_ACCOUNT_STEPUP_REFUSED,
    BANK_ACCOUNT_VERIFICATION_RESULT,
    /** A platform administrator looked at a garage's (masked) bank account. */
    PLATFORM_BANK_ACCOUNT_VIEWED,
    EMERGENCY_REQUEST_CREATED,
    EMERGENCY_REQUEST_ACCEPTED,
    EMERGENCY_REQUEST_REJECTED,
    EMERGENCY_REQUEST_COMPLETED,
    EMERGENCY_REQUEST_CANCELLED,
    /** An Emergency Assist submission was refused because the garage was not eligible (no silent re-routing). */
    EMERGENCY_REQUEST_REFUSED,
    /** Trip evidence was moved into its job card's folder (old and new key, mode and whether the old object was kept in the metadata). */
    TRIP_EVIDENCE_ADOPTED,
    /** Moving trip evidence into its job card's folder failed or found neither object (reason class only; a later run retries). */
    TRIP_EVIDENCE_ADOPTION_FAILED
}
