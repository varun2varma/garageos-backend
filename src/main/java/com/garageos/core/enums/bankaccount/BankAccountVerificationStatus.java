package com.garageos.core.enums.bankaccount;

/**
 * Where a registered bank account stands. Registration, ownership verification, beneficiary activation and payouts
 * are four different things; this status only speaks to ownership verification, and ONLY a verification provider's
 * result may move it off {@link #PENDING_VERIFICATION}. A phone or e-mail OTP does not prove bank ownership.
 */
public enum BankAccountVerificationStatus {
    PENDING_VERIFICATION,
    VERIFIED,
    FAILED,
    NEEDS_REVIEW
}
