package com.garageos.modules.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCatalogItemRequest {

    /** PART or SERVICE. */
    @NotBlank
    private String itemType;

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String description;

    @NotBlank
    @Size(max = 20)
    private String defaultUnit;

    @Size(max = 60)
    private String category;

    /** HSN for a PART, SAC for a SERVICE. Optional until the item is classified. */
    @Pattern(regexp = "^[0-9]{4,8}$", message = "HSN/SAC must be 4 to 8 digits.")
    private String hsnSacCode;

    @Size(max = 40)
    private String taxRuleCode;

    /** True only when a reviewer confirms the code and tax rule. */
    @NotNull
    private Boolean verified = Boolean.FALSE;

    /** Required when verified: where the code and rate were confirmed. */
    @Size(max = 255)
    private String classificationSource;
}
