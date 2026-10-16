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
 *
 * {@code paymentStatus} is the invoice's own payment status, or null when it has none.
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
        String paymentStatus,
        InvoiceTaxCalculator.Breakdown breakdown,
        InvoiceSnapshotView snapshot,
        byte[] signatureBytes,
        byte[] stampBytes) {

    /** Without the garage authorization images (legacy invoices / unit tests): the blank signatory line is printed. */
    public InvoiceDocumentData(
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
            String paymentStatus,
            InvoiceTaxCalculator.Breakdown breakdown,
            InvoiceSnapshotView snapshot) {
        this(garage, logoBytes, invoiceNumber, invoiceDate, jobCardNumber, vehicleNumber, serviceAdvisor,
                customerName, customerMobile, customerAddress, vehicleDescription, vehicleDetails,
                remarks, paymentStatus, breakdown, snapshot, null, null);
    }

    /** Legacy invoice: no persisted tax snapshot, the renderer uses the breakdown only. */
    public InvoiceDocumentData(
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
            String paymentStatus,
            InvoiceTaxCalculator.Breakdown breakdown) {
        this(garage, logoBytes, invoiceNumber, invoiceDate, jobCardNumber, vehicleNumber, serviceAdvisor,
                customerName, customerMobile, customerAddress, vehicleDescription, vehicleDetails,
                remarks, paymentStatus, breakdown, null);
    }
}
