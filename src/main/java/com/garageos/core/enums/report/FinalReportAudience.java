package com.garageos.core.enums.report;

/** Who a final Job Card report is written for. Never inferred from client input. */
public enum FinalReportAudience {
    /** Complete workshop record for the garage owner / manager. */
    OWNER,
    /** Customer-safe service history. */
    CUSTOMER
}
