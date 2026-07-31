package com.garageos.modules.customer.dto.response.portal;

import com.garageos.core.enums.InvoiceStatus;
import com.garageos.core.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CustomerInvoiceResponse {

    private Long id;

    private String invoiceNumber;

    private String estimateNumber;

    private InvoiceStatus invoiceStatus;

    private PaymentStatus paymentStatus;

    private BigDecimal grandTotal;

    private LocalDateTime generatedAt;

}