package com.garageos.modules.payment.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Real aggregates over recorded payments / issued invoices for a period (inclusive dates). */
@Getter
@Builder
public class PaymentSummaryResponse {

    private LocalDate from;

    private LocalDate to;

    private BigDecimal collectedTotal;

    private long paymentCount;

    private List<MethodTotal> byMethod;

    /** Invoices issued and still awaiting full payment (all time, not period-bound). */
    private long outstandingInvoiceCount;

    private BigDecimal outstandingAmount;

    /** Invoices issued in the period (GST / invoice summary). */
    private long invoicesIssuedCount;

    private BigDecimal invoicesSubtotal;

    private BigDecimal invoicesDiscount;

    private BigDecimal invoicesGst;

    private BigDecimal invoicesGrandTotal;

    @Getter
    @Builder
    public static class MethodTotal {

        private String method;

        private long count;

        private BigDecimal total;
    }
}
