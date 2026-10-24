package com.garageos.modules.bankaccount.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.bankaccount.dto.request.RegisterBankAccountRequest;
import com.garageos.modules.bankaccount.dto.request.RetireBankAccountRequest;
import com.garageos.modules.bankaccount.dto.response.BankAccountView;
import com.garageos.modules.bankaccount.service.BankAccountService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * A garage owner's payout-account registration. OWNER only (the service additionally requires the caller to own THIS
 * garage), every response is masked, and changes need the owner's password again. A platform administrator reads the
 * masked status through {@code GET /api/v1/platform/garages/{id}/bank-account} instead.
 */
@RestController
@RequestMapping("/api/v1/garages/{garageId}/bank-account")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class BankAccountController {

    private final BankAccountService service;

    @GetMapping
    public ResponseEntity<ApiResponse<BankAccountView>> current(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long garageId) {
        return ApiResponseUtil.success("Bank account fetched.", service.currentForOwner(user, garageId).orElse(null));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<BankAccountView>> register(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long garageId,
            @Valid @RequestBody RegisterBankAccountRequest request) {
        return ApiResponseUtil.success("Bank account registered. Verification is pending.",
                service.register(user, garageId, request));
    }

    @PostMapping("/retire")
    public ResponseEntity<ApiResponse<Void>> retire(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long garageId,
            @Valid @RequestBody RetireBankAccountRequest request) {
        service.retire(user, garageId, request);
        return ApiResponseUtil.success("Bank account removed.");
    }
}
