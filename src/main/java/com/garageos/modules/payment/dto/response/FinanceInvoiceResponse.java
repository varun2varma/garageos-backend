package com.garageos.modules.payment.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Invoice row for the finance personas (cashier payable list, accountant invoices). */
@Getter
@Builder
public class FinanceInvoiceResponse {

    private Long invoiceId;

    private String invoiceNumber;

    private String jobCardNumber;

    private String customerName;

    private String vehicleRegistration;

    private String invoiceStatus;

    private String paymentStatus;

    private BigDecimal subtotal;

    private BigDecimal discount;

    private BigDecimal gst;

    private BigDecimal grandTotal;

    private BigDecimal paidAmount;

    private BigDecimal outstandingAmount;

    private LocalDateTime generatedAt;
}
