package com.garageos.modules.garage.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * The garage palette. Both values are optional and null/blank clears that colour (default theme). The exact format
 * is validated and normalised server-side by {@code BrandColors}; the size cap just keeps junk out of the parser.
 */
@Getter
@Setter
public class UpdateBrandingThemeRequest {

    @Size(max = 16, message = "Invalid primary colour.")
    private String primaryColor;

    @Size(max = 16, message = "Invalid secondary colour.")
    private String secondaryColor;
}
