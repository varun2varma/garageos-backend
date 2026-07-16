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

    private String invoiceStatus;

    private String paymentStatus;

    private BigDecimal subtotal;

    private BigDecimal discount;

    private BigDecimal gst;

    private BigDecimal grandTotal;

    private String remarks;
}