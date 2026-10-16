package com.garageos.core.enums;

/** Lifecycle of an Owner credit authorization. Independent of the invoice's payment status. */
public enum CreditAuthorizationStatus {

    /** Valid: the vehicle may be released while the invoice is unpaid. */
    ACTIVE,

    /** Withdrawn by the Owner before settlement; no longer permits release. */
    REVOKED,

    /** The invoice became fully PAID; the authorization is closed. */
    SETTLED
}
