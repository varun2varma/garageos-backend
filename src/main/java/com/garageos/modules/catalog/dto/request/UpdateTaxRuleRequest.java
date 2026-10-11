package com.garageos.modules.catalog.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Rate and effective dates are immutable (add a new effective-dated rate instead);
 * only the description, the source and the active flag can change.
 */
@Getter
@Setter
public class UpdateTaxRuleRequest {

    @Size(max = 255)
    private String description;

    @Size(max = 255)
    private String sourceReference;

    private LocalDate verifiedOn;

    @NotNull
    private Boolean active = Boolean.TRUE;
}
