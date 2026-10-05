package com.garageos.modules.payment.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One payment record, doubling as the receipt data (a receipt PDF is not generated yet). */
@Getter
@Builder
public class PaymentResponse {

    private Long id;

    private String receiptNumber;

    private Long invoiceId;

    private String invoiceNumber;

    private String jobCardNumber;

    private String customerName;

    private String vehicleRegistration;

    private String garageName;

    private BigDecimal amount;

    private String method;

    private String reference;

    private Long receivedByUserId;

    private String receivedByName;

    private LocalDateTime paidAt;

    private String status;

    /** State of the invoice right after this payment (as of now when listing). */
    private BigDecimal invoiceGrandTotal;

    private BigDecimal invoicePaidTotal;

    private BigDecimal invoiceOutstanding;

    private String invoicePaymentStatus;
}
