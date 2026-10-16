package com.garageos.modules.credit.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.credit.dto.AuthorizeCreditRequest;
import com.garageos.modules.credit.dto.CreditAuthorizationResponse;
import com.garageos.modules.credit.dto.RevokeCreditRequest;
import com.garageos.modules.credit.service.InvoiceCreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Owner credit authorization. Only the Owner authorizes / revokes (a Manager is refused by the annotation AND by
 * the service). Owner, Accountant and Cashier may read balances and statuses; repayments are ordinary payments
 * (POST /api/v1/payments) and are not recorded here.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InvoiceCreditController {

    private static final String OWNER_ONLY = "hasRole('OWNER')";
    private static final String FINANCE_READ = "hasAnyRole('OWNER', 'ACCOUNTANT', 'CASHIER')";

    private final InvoiceCreditService service;

    @PostMapping("/invoices/{invoiceId}/credit")
    @PreAuthorize(OWNER_ONLY)
    public ResponseEntity<ApiResponse<CreditAuthorizationResponse>> authorize(
            @PathVariable Long invoiceId,
            @Valid @RequestBody AuthorizeCreditRequest request) {

        return ApiResponseUtil.success("Credit authorized.", service.authorize(invoiceId, request));
    }

    @PostMapping("/invoices/{invoiceId}/credit/revoke")
    @PreAuthorize(OWNER_ONLY)
    public ResponseEntity<ApiResponse<CreditAuthorizationResponse>> revoke(
            @PathVariable Long invoiceId,
            @Valid @RequestBody RevokeCreditRequest request) {

        return ApiResponseUtil.success("Credit revoked.", service.revoke(invoiceId, request));
    }

    @GetMapping("/invoices/{invoiceId}/credit")
    @PreAuthorize(FINANCE_READ)
    public ResponseEntity<ApiResponse<List<CreditAuthorizationResponse>>> history(@PathVariable Long invoiceId) {

        return ApiResponseUtil.success("Credit history fetched.", service.history(invoiceId));
    }

    @GetMapping("/credits/active")
    @PreAuthorize(FINANCE_READ)
    public ResponseEntity<ApiResponse<List<CreditAuthorizationResponse>>> active() {

        return ApiResponseUtil.success("Active credit fetched.", service.listActive());
    }
}
