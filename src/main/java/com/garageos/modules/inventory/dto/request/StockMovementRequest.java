package com.garageos.modules.inventory.dto.request;

import com.garageos.core.enums.inventory.StockMovementType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockMovementRequest {

    @NotNull
    private StockMovementType type;

    /**
     * STOCK_IN / STOCK_OUT: a positive amount. ADJUSTMENT: a signed,
     * non-zero delta (negative reduces stock). Validated in the service.
     */
    @NotNull
    @Digits(integer = 9, fraction = 3)
    private BigDecimal quantity;

    /** Reference / reason. Mandatory for ADJUSTMENT. */
    @Size(max = 255)
    private String reason;

    /** STOCK_OUT only: issue the part against this job card (same garage). */
    @Size(max = 60)
    private String jobCardNumber;
}
