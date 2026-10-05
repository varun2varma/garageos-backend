package com.garageos.modules.payment.dto.request;

import com.garageos.core.enums.payment.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RecordPaymentRequest {

    @NotNull
    private Long invoiceId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero.")
    @Digits(integer = 10, fraction = 2, message = "Amount may have at most 2 decimal places.")
    private BigDecimal amount;

    @NotNull
    private PaymentMethod method;

    @Size(max = 100)
    private String reference;

    /**
     * Client-generated unique key for this submission attempt. A retry
     * (double tap, dropped response) with the same key returns the
     * already-recorded payment instead of recording a second one.
     */
    @Size(max = 80)
    private String idempotencyKey;
}
