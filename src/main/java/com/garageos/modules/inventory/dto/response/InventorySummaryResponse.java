package com.garageos.modules.inventory.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Real counts for one garage's active inventory items. */
@Getter
@Builder
public class InventorySummaryResponse {

    private long totalActiveItems;

    private long lowStockCount;

    private long outOfStockCount;
}
