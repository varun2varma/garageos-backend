package com.garageos.modules.inventory.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class InventoryItemResponse {

    private Long id;

    private Long garageId;

    private String name;

    private String partNumber;

    private String unit;

    private BigDecimal quantity;

    private BigDecimal reorderLevel;

    private boolean active;

    private boolean lowStock;

    private boolean outOfStock;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
