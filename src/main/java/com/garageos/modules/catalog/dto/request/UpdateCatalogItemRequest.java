package com.garageos.modules.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCatalogItemRequest {

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

    @Pattern(regexp = "^[0-9]{4,8}$", message = "HSN/SAC must be 4 to 8 digits.")
    private String hsnSacCode;

    @Size(max = 40)
    private String taxRuleCode;

    @NotNull
    private Boolean verified = Boolean.FALSE;

    @NotNull
    private Boolean active = Boolean.TRUE;

    @Size(max = 255)
    private String classificationSource;

    /** The version the editor loaded; a stale value is rejected so edits never overwrite each other. */
    private Long version;
}
