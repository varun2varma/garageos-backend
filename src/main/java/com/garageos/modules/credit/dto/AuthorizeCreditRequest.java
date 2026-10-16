package com.garageos.modules.credit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class AuthorizeCreditRequest {

    /** Amount of the invoice the Owner allows to stay unpaid; must not exceed the outstanding balance. */
    @NotNull
    @Positive
    private BigDecimal amount;

    /** Mandatory; must be after today. */
    @NotNull
    private LocalDate dueDate;

    @NotBlank
    @Size(max = 500)
    private String reason;
}
