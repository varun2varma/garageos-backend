package com.garageos.modules.invoice.pdf;

import com.garageos.core.util.InvoiceTaxCalculator;
import com.garageos.modules.garage.dto.response.GarageBrandingResponse;

import java.time.LocalDate;

/**
 * Everything the PDF renderer needs, already resolved from authoritative
 * domain data (Garage via JobCard.garageId, the Invoice, its final billable
 * items). The renderer itself never touches the database.
 *
 * {@code logoBytes} is null for a garage without a logo; the renderer then
 * falls back to the GarageST default mark.
 */
public record InvoiceDocumentData(
        GarageBrandingResponse garage,
        byte[] logoBytes,
        String invoiceNumber,
        LocalDate invoiceDate,
        String jobCardNumber,
        String vehicleNumber,
        String serviceAdvisor,
        String customerName,
        String customerMobile,
        String customerAddress,
        String vehicleDescription,
        String vehicleDetails,
        String remarks,
        InvoiceTaxCalculator.Breakdown breakdown) {
}
