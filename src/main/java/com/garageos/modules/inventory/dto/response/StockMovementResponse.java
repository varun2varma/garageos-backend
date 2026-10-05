package com.garageos.modules.inventory.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class StockMovementResponse {

    private Long id;

    private Long itemId;

    private String itemName;

    private String partNumber;

    private String unit;

    private String type;

    /** Signed delta applied to the item quantity. */
    private BigDecimal quantity;

    private BigDecimal balanceAfter;

    private String reason;

    private String jobCardNumber;

    private Long performedByUserId;

    private String performedByName;

    private LocalDateTime createdAt;
}
