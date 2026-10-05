package com.garageos.core.enums.otp;

public enum OtpStatus {
    /** Issued and still redeemable. */
    PENDING,
    /** Redeemed successfully (one-time use). */
    VERIFIED,
    /** Expired before redemption. */
    EXPIRED,
    /** Too many wrong attempts. */
    LOCKED,
    /** Replaced by a newer OTP for the same destination and purpose. */
    SUPERSEDED
}
