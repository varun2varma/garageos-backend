package com.garageos.modules.invoice.service;

import com.garageos.modules.identity.security.principal.GarageUserPrincipal;

public interface InvoicePdfService {

    /**
     * Generates the authoritative tax-invoice PDF for one invoice. Staff must
     * belong to the invoice's garage; a customer must own the invoice's job
     * card. Branding is resolved JobCard -> garageId -> Garage, never from the
     * caller. Anyone else gets the same not-found used elsewhere.
     */
    InvoicePdf generate(GarageUserPrincipal principal, Long invoiceId);

    record InvoicePdf(byte[] content, String fileName) {
    }
}
