package com.garageos.modules.bankaccount.provider;

import com.garageos.core.enums.bankaccount.BankAccountVerificationStatus;

/**
 * Provider-neutral boundary for proving that a bank account belongs to the named holder (penny-drop or name-match
 * style checks). No provider is selected or integrated yet: the default {@link UnconfiguredBankVerificationProvider}
 * reports {@code isConfigured() == false} and registration then simply stays {@code PENDING_VERIFICATION}.
 *
 * <p>A real implementation lives behind this interface, takes its credentials from the environment, and returns an
 * outcome only when the PROVIDER said so. It must never be given an OTP or a customer-supplied verdict, and nothing
 * outside it may mark an account VERIFIED. Beneficiary activation and payouts are further, separate capabilities that
 * would get their own interfaces.
 */
public interface BankVerificationProvider {

    boolean isConfigured();

    /** Short stable name recorded on the account, for example {@code "RAZORPAYX"}. */
    String name();

    /**
     * @param accountNumber the plain account number, supplied only for the duration of this call
     * @throws RuntimeException on transport or provider failure; the caller leaves the account PENDING_VERIFICATION
     *                          (an outage is not a failed verification)
     */
    Outcome verify(String accountHolderName, String accountNumber, String ifsc);

    /** What the provider decided. {@code reference} is the provider's own identifier for audit and support. */
    record Outcome(BankAccountVerificationStatus status, String reference, String failureReason) {
    }
}
