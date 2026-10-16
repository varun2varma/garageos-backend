package com.garageos.modules.credit.service;

import com.garageos.core.enums.CreditAuthorizationStatus;
import com.garageos.core.enums.PaymentStatus;
import com.garageos.modules.credit.repository.InvoiceCreditAuthorizationRepository;
import com.garageos.modules.invoice.entity.Invoice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * THE rule for "may this invoice's vehicle be released (readiness, delivery, closure, collection choice)?".
 * One definition replaces the payment-only checks that used to be duplicated across delivery, closure and
 * return-preference:
 *
 *   cleared  =  invoice is fully PAID   OR   it has an ACTIVE Owner credit authorization.
 *
 * Clearance is about RELEASE only. It never changes payment status and it is deliberately NOT used for the
 * invoice PDF, which stays PAID-only.
 */
@Component
@RequiredArgsConstructor
public class ReleaseClearance {

    private final InvoiceCreditAuthorizationRepository creditRepository;

    public boolean isCleared(Invoice invoice) {

        if (invoice == null) {
            return false;
        }

        if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
            return true;
        }

        return invoice.getId() != null
                && creditRepository.existsByInvoiceIdAndStatus(invoice.getId(), CreditAuthorizationStatus.ACTIVE);
    }
}
