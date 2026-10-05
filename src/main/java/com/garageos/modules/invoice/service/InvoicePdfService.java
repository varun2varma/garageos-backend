package com.garageos.modules.invoice.service;

import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.invoice.entity.Invoice;

public interface InvoicePdfService {

    /**
     * Generates the authoritative tax-invoice PDF for one invoice. Staff must
     * belong to the invoice's garage; a customer must own the invoice's job
     * card. Branding is resolved JobCard -> garageId -> Garage, never from the
     * caller. Anyone else gets the same not-found used elsewhere.
     */
    InvoicePdf generate(GarageUserPrincipal principal, Long invoiceId);

    /**
     * The tax breakdown exactly as the invoice PDF prints it (same final
     * billable lines, same calculator). No authorization: callers authorize
     * the job card first. Lets other documents quote invoice figures without
     * re-implementing any invoice logic.
     */
    com.garageos.core.util.InvoiceTaxCalculator.Breakdown breakdownFor(Invoice invoice);

    record InvoicePdf(byte[] content, String fileName) {
    }
}
