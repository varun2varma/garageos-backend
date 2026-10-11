package com.garageos.modules.catalog.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateTaxRuleRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_]{2,40}$", message = "Rule code: letters, digits and underscore only.")
    private String ruleCode;

    @Size(max = 255)
    private String description;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal ratePercent;

    @NotNull
    private LocalDate effectiveFrom;

    /** Where the rate was confirmed. */
    @Size(max = 255)
    private String sourceReference;

    private LocalDate verifiedOn;
}
