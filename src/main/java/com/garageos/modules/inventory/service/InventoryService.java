package com.garageos.modules.inventory.service;

import com.garageos.core.enums.inventory.StockMovementType;
import com.garageos.modules.inventory.dto.request.CreateInventoryItemRequest;
import com.garageos.modules.inventory.dto.request.StockMovementRequest;
import com.garageos.modules.inventory.dto.request.UpdateInventoryItemRequest;
import com.garageos.modules.inventory.dto.response.InventoryItemResponse;
import com.garageos.modules.inventory.dto.response.InventorySummaryResponse;
import com.garageos.modules.inventory.dto.response.StockMovementResponse;
import org.springframework.data.domain.Page;

public interface InventoryService {

    InventoryItemResponse createItem(CreateInventoryItemRequest request);

    InventoryItemResponse updateItem(Long id, UpdateInventoryItemRequest request);

    InventoryItemResponse getItem(Long id, Long garageId);

    /** filter: ALL (default), LOW, OUT. */
    Page<InventoryItemResponse> listItems(
            Long garageId, String search, String filter, boolean includeInactive,
            int page, int size);

    InventorySummaryResponse getSummary(Long garageId);

    InventoryItemResponse recordMovement(Long itemId, StockMovementRequest request);

    Page<StockMovementResponse> listMovements(
            Long garageId, Long itemId, StockMovementType type, String jobCardNumber,
            int page, int size);
}
