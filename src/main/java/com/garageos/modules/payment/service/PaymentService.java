package com.garageos.modules.payment.service;

import com.garageos.core.enums.PaymentStatus;
import com.garageos.core.enums.payment.PaymentMethod;
import com.garageos.modules.payment.dto.request.RecordPaymentRequest;
import com.garageos.modules.payment.dto.response.FinanceInvoiceResponse;
import com.garageos.modules.payment.dto.response.PaymentResponse;
import com.garageos.modules.payment.dto.response.PaymentSummaryResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface PaymentService {

    /** Records a full or partial payment; idempotent per idempotencyKey; never over-collects. */
    PaymentResponse recordPayment(RecordPaymentRequest request);

    PaymentResponse getPayment(Long id);

    /** Payments of an invoice the caller may see (garage staff, or the owning customer). */
    List<PaymentResponse> getInvoicePayments(Long invoiceId);

    Page<PaymentResponse> getPayments(
            Long garageId, boolean allGarages, LocalDate from, LocalDate to,
            PaymentMethod method, int page, int size);

    PaymentSummaryResponse getSummary(
            Long garageId, boolean allGarages, LocalDate from, LocalDate to);

    /** Invoices with their paid / outstanding amounts. payableOnly = ready for payment collection. */
    Page<FinanceInvoiceResponse> getInvoices(
            Long garageId, boolean allGarages, boolean payableOnly,
            PaymentStatus paymentStatus, int page, int size);
}
