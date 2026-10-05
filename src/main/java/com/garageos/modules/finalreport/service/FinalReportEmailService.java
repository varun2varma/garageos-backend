package com.garageos.modules.finalreport.service;

/**
 * Delivers an issued final report by e-mail: the customer report to the
 * customer, the owner report to the garage owner - never crossed. Delivery
 * state lives on the report row, so a failure is retried with backoff and
 * never touches the Job Card.
 */
public interface FinalReportEmailService {

    /**
     * Attempts to send one report's e-mail if it is due. Safe to call from any
     * thread and any number of times: the report is claimed atomically, and
     * every failure is recorded rather than thrown.
     */
    void dispatch(Long reportId);
}
