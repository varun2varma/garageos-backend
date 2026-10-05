package com.garageos.modules.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** Catalogue fields only - quantity changes exclusively through stock movements. */
@Data
public class UpdateInventoryItemRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 60)
    private String partNumber;

    @NotBlank
    @Size(max = 20)
    private String unit;

    @NotNull
    @DecimalMin("0")
    @Digits(integer = 9, fraction = 3)
    private BigDecimal reorderLevel;

    @NotNull
    private Boolean active;
}
