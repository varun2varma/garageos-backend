package com.garageos.modules.catalog.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Classification review of one catalog item: propose a code and rule, or verify them with their source. */
@Getter
@Setter
public class ClassifyCatalogItemRequest {

    @Pattern(regexp = "^[0-9]{4,8}$", message = "HSN/SAC must be 4 to 8 digits.")
    private String hsnSacCode;

    @Size(max = 40)
    private String taxRuleCode;

    /** True only when the reviewer has confirmed the code and rate from an authoritative source. */
    @NotNull
    private Boolean verified = Boolean.FALSE;

    /** Required when verified: where the classification was confirmed. */
    @Size(max = 255)
    private String classificationSource;

    private Long version;
}
