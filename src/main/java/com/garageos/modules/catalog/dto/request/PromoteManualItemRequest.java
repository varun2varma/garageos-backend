package com.garageos.modules.catalog.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Every field is optional: name and unit default to the manual line's own values. */
@Getter
@Setter
public class PromoteManualItemRequest {

    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String description;

    @Size(max = 20)
    private String defaultUnit;

    @Pattern(regexp = "^[0-9]{4,8}$", message = "HSN/SAC must be 4 to 8 digits.")
    private String hsnSacCode;

    @Size(max = 40)
    private String taxRuleCode;

    private Boolean verified = Boolean.FALSE;

    @Size(max = 60)
    private String category;

    @Size(max = 255)
    private String classificationSource;
}
