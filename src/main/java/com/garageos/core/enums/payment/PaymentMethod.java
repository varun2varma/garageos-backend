package com.garageos.core.enums.payment;

public enum PaymentMethod {

    CASH,
    UPI,
    CARD,
    BANK_TRANSFER,

    /** Method was never captured (legacy "mark as paid" flow and migrated historical payments). */
    UNSPECIFIED;

    /** Methods a cashier may explicitly choose when recording a payment. */
    public boolean isSelectable() {
        return this != UNSPECIFIED;
    }
}
