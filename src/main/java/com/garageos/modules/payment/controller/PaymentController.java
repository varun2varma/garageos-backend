package com.garageos.modules.payment.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.core.enums.PaymentStatus;
import com.garageos.core.enums.payment.PaymentMethod;
import com.garageos.modules.payment.dto.request.RecordPaymentRequest;
import com.garageos.modules.payment.dto.response.FinanceInvoiceResponse;
import com.garageos.modules.payment.dto.response.PaymentResponse;
import com.garageos.modules.payment.dto.response.PaymentSummaryResponse;
import com.garageos.modules.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Payment collection and financial read APIs.
 *
 * Authority is enforced here (role) AND in the service (garage scope):
 * CASHIER collects; ACCOUNTANT only reads/reviews; MANAGER, SERVICE_ADVISOR
 * and OWNER keep their existing payment authority. No Flutter persona is
 * involved - the server decides from the authenticated roles only.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    /** Who may record a payment. ACCOUNTANT deliberately excluded (review only). */
    // ACCOUNTANT may record repayments only against invoices with an active Owner credit authorization; that
    // narrowing is enforced in PaymentServiceImpl.requireRepaymentScopeForAccountant, not by this annotation.
    private static final String COLLECT_ROLES =
            "hasAnyRole('CASHIER', 'MANAGER', 'SERVICE_ADVISOR', 'OWNER', 'ACCOUNTANT')";

    private static final String READ_ROLES =
            "hasAnyRole('CASHIER', 'ACCOUNTANT', 'MANAGER', 'SERVICE_ADVISOR', 'OWNER')";

    private final PaymentService service;

    @PostMapping
    @PreAuthorize(COLLECT_ROLES)
    public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(
            @Valid @RequestBody RecordPaymentRequest request) {

        return ApiResponseUtil.created(
                "Payment recorded successfully.",
                service.recordPayment(request));
    }

    @GetMapping
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPayments(
            @RequestParam(required = false) Long garageId,
            @RequestParam(defaultValue = "false") boolean allGarages,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponseUtil.success(
                "Payments fetched successfully.",
                service.getPayments(garageId, allGarages, from, to, method, page, size));
    }

    @GetMapping("/summary")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<ApiResponse<PaymentSummaryResponse>> getSummary(
            @RequestParam(required = false) Long garageId,
            @RequestParam(defaultValue = "false") boolean allGarages,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ApiResponseUtil.success(
                "Payment summary fetched successfully.",
                service.getSummary(garageId, allGarages, from, to));
    }

    @GetMapping("/invoices")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<ApiResponse<Page<FinanceInvoiceResponse>>> getInvoices(
            @RequestParam(required = false) Long garageId,
            @RequestParam(defaultValue = "false") boolean allGarages,
            @RequestParam(defaultValue = "false") boolean payableOnly,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponseUtil.success(
                "Invoices fetched successfully.",
                service.getInvoices(garageId, allGarages, payableOnly, paymentStatus, page, size));
    }

    /** Staff of the invoice garage or the owning customer (checked in the service). */
    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getInvoicePayments(
            @PathVariable Long invoiceId) {

        return ApiResponseUtil.success(
                "Invoice payments fetched successfully.",
                service.getInvoicePayments(invoiceId));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable Long id) {

        return ApiResponseUtil.success(
                "Payment fetched successfully.",
                service.getPayment(id));
    }
}
