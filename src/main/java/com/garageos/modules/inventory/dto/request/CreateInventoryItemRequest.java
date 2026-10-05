package com.garageos.modules.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateInventoryItemRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    /** Part number / SKU, unique per garage when supplied. */
    @Size(max = 60)
    private String partNumber;

    @NotBlank
    @Size(max = 20)
    private String unit;

    /** Optional opening stock; recorded as a STOCK_IN movement. */
    @DecimalMin("0")
    @Digits(integer = 9, fraction = 3)
    private BigDecimal openingQuantity;

    @DecimalMin("0")
    @Digits(integer = 9, fraction = 3)
    private BigDecimal reorderLevel;
}
