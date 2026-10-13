package com.garageos.modules.invoice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class InvoiceResponse {

    private Long id;

    private String invoiceNumber;

    private Long estimateId;

    /** Business number of the estimate this invoice bills (e.g. G001-EST-2026-000001); estimateId stays the API key. */
    private String estimateNumber;

    private String invoiceStatus;

    private String paymentStatus;

    private BigDecimal subtotal;

    private BigDecimal discount;

    private BigDecimal gst;

    private BigDecimal grandTotal;

    private String remarks;

    // Canonical tax figures; null for invoices issued before per-line snapshots existed.
    // gst keeps its legacy meaning (GST added on top of tax-exclusive lines).
    private BigDecimal taxTotal;

    private BigDecimal cgstTotal;

    private BigDecimal sgstTotal;

    private BigDecimal igstTotal;

    /** Whole-rupee round-off already included in grandTotal. */
    private BigDecimal roundOff;

    private String supplyType;

    private String placeOfSupplyState;

    private boolean taxSnapshot;
}