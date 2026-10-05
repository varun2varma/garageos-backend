package com.garageos.core.enums.report;

public enum FinalReportEmailStatus {
    /** Waiting to be sent (or to be retried). */
    PENDING,
    /** Claimed by a sender; recovered automatically if the sender dies. */
    SENDING,
    SENT,
    /** Last attempt failed; retried with backoff until the attempt limit. */
    FAILED,
    /** Nothing to send: no recipient address, or a regenerated version (never auto-emailed). */
    SKIPPED
}
