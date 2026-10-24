package com.garageos.modules.bankaccount.service;

import com.garageos.modules.bankaccount.dto.request.RegisterBankAccountRequest;
import com.garageos.modules.bankaccount.dto.request.RetireBankAccountRequest;
import com.garageos.modules.bankaccount.dto.response.BankAccountView;
import com.garageos.modules.bankaccount.provider.BankVerificationProvider;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;

import java.util.Optional;

/**
 * Bank-account REGISTRATION for a garage. It records and protects the account; it does not verify ownership by itself
 * (a {@link BankVerificationProvider} would), it does not activate a payout beneficiary, and it moves no money.
 */
public interface BankAccountService {

    /** Owner-only; step-up (password) required; replaces any current account, keeping it as history. */
    BankAccountView register(GarageUserPrincipal principal, Long garageId, RegisterBankAccountRequest request);

    /** Owner-only masked view of the current account. */
    Optional<BankAccountView> currentForOwner(GarageUserPrincipal principal, Long garageId);

    /** Owner-only; step-up required; the account stops being current (no payouts to it). History is kept. */
    void retire(GarageUserPrincipal principal, Long garageId, RetireBankAccountRequest request);

    /** Platform administrator's masked view; every call is itself audited. */
    Optional<BankAccountView> currentForPlatform(Long garageId, Long adminUserId);

    /** Registration status only (no values), for an admin summary. {@code NOT_REGISTERED} when there is none. */
    String statusSummary(Long garageId);

    /**
     * Applies a provider's verdict. Only ever called with a provider's own outcome (never from a request); an
     * outage is not an outcome and must not be passed here.
     */
    void applyVerificationOutcome(Long accountId, String providerName, BankVerificationProvider.Outcome outcome);
}
