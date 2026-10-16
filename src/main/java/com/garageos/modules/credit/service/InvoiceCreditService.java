package com.garageos.modules.credit.service;

import com.garageos.modules.credit.dto.AuthorizeCreditRequest;
import com.garageos.modules.credit.dto.CreditAuthorizationResponse;
import com.garageos.modules.credit.dto.RevokeCreditRequest;
import com.garageos.modules.invoice.entity.Invoice;

import java.util.List;

public interface InvoiceCreditService {

    /** Owner only. Records the authorization, then moves the job to READY_FOR_DELIVERY if it is waiting for release. */
    CreditAuthorizationResponse authorize(Long invoiceId, AuthorizeCreditRequest request);

    /** Owner only. Withdraws an ACTIVE authorization. */
    CreditAuthorizationResponse revoke(Long invoiceId, RevokeCreditRequest request);

    /** History of the invoice's authorizations (newest first), garage-scoped. */
    List<CreditAuthorizationResponse> history(Long invoiceId);

    /** All ACTIVE authorizations of the caller's garage, soonest due first (finance dashboard). */
    List<CreditAuthorizationResponse> listActive();

    /**
     * Called inside the payment transaction after a payment/repayment was recorded. Audits the repayment
     * and, when the invoice is now fully PAID, settles the ACTIVE authorization. No-op without one. Never
     * changes the invoice's payment status.
     */
    void onPaymentRecorded(Invoice invoice, java.math.BigDecimal amount, Long actorUserId);
}
