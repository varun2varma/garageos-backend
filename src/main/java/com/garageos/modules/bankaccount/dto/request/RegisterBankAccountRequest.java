package com.garageos.modules.bankaccount.dto.request;

import com.garageos.core.enums.bankaccount.BankAccountType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Registers (or replaces) the garage's payout account. Holds the plain account number and the owner's password; the
 * class overrides nothing that could print them and the service never logs the request.
 */
@Getter
@Setter
public class RegisterBankAccountRequest {

    @NotBlank(message = "Account holder name is required.")
    @Size(min = 3, max = 100, message = "Account holder name must be 3 to 100 characters.")
    private String accountHolderName;

    @NotBlank(message = "Account number is required.")
    @Pattern(regexp = "^[0-9]{9,18}$", message = "Account number must be 9 to 18 digits.")
    private String accountNumber;

    @NotBlank(message = "IFSC is required.")
    @Pattern(regexp = "^[A-Za-z]{4}0[A-Za-z0-9]{6}$", message = "Invalid IFSC.")
    private String ifsc;

    @Size(max = 100)
    private String bankName;

    @NotNull(message = "Account type is required.")
    private BankAccountType accountType;

    /** The owner confirms the account is theirs to receive payouts into. */
    @AssertTrue(message = "Consent is required.")
    private boolean consent;

    /** Step-up: the owner's current password, verified server-side before anything is stored. */
    @NotBlank(message = "Your current password is required to change bank details.")
    private String password;

    @Override
    public String toString() {
        return "RegisterBankAccountRequest[redacted]";
    }
}
