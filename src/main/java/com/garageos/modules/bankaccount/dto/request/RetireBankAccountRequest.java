package com.garageos.modules.bankaccount.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RetireBankAccountRequest {

    /** Step-up: the owner's current password. */
    @NotBlank(message = "Your current password is required to change bank details.")
    private String password;

    @Override
    public String toString() {
        return "RetireBankAccountRequest[redacted]";
    }
}
