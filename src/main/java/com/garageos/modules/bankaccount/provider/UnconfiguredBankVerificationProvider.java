package com.garageos.modules.bankaccount.provider;

import org.springframework.stereotype.Component;

/**
 * Default when no bank-verification provider has been integrated: every account stays PENDING_VERIFICATION. The
 * service uses the first CONFIGURED provider it finds, so a real provider simply takes precedence over this one.
 */
@Component
public class UnconfiguredBankVerificationProvider implements BankVerificationProvider {

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public String name() {
        return "NONE";
    }

    @Override
    public Outcome verify(String accountHolderName, String accountNumber, String ifsc) {
        throw new UnsupportedOperationException("No bank verification provider is configured.");
    }
}
