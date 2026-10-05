package com.garageos.core.enums.otp;

/**
 * What an OTP is FOR. A challenge is always bound to exactly one purpose, so
 * an OTP issued for one flow can never be redeemed by another.
 *
 * Only USER_REGISTRATION and GARAGE_REGISTRATION are wired to a flow today;
 * the others reserve the value so adding the flow later needs no schema or
 * service change.
 */
public enum OtpPurpose {
    USER_REGISTRATION,
    GARAGE_REGISTRATION,
    PHONE_CHANGE,
    PASSWORD_RESET,
    LOGIN,
    PAYMENT_VERIFICATION
}
