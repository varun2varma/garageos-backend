package com.garageos.modules.inventory.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.core.enums.inventory.StockMovementType;
import com.garageos.modules.inventory.dto.request.CreateInventoryItemRequest;
import com.garageos.modules.inventory.dto.request.StockMovementRequest;
import com.garageos.modules.inventory.dto.request.UpdateInventoryItemRequest;
import com.garageos.modules.inventory.dto.response.InventoryItemResponse;
import com.garageos.modules.inventory.dto.response.InventorySummaryResponse;
import com.garageos.modules.inventory.dto.response.StockMovementResponse;
import com.garageos.modules.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Inventory APIs. Only INVENTORY_MANAGER may change stock or the catalogue;
 * OWNER and MANAGER get read-only oversight; TECHNICIAN / SERVICE_ADVISOR
 * may look parts up. Garage isolation is enforced in the service.
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private static final String WRITE_ROLES = "hasRole('INVENTORY_MANAGER')";

    private static final String OVERSIGHT_ROLES =
            "hasAnyRole('INVENTORY_MANAGER', 'OWNER', 'MANAGER')";

    private static final String LOOKUP_ROLES =
            "hasAnyRole('INVENTORY_MANAGER', 'OWNER', 'MANAGER', 'SERVICE_ADVISOR', 'TECHNICIAN')";

    private final InventoryService service;

    @PostMapping("/items")
    @PreAuthorize(WRITE_ROLES)
    public ResponseEntity<ApiResponse<InventoryItemResponse>> createItem(
            @Valid @RequestBody CreateInventoryItemRequest request) {

        return ApiResponseUtil.created("Part created successfully.", service.createItem(request));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize(WRITE_ROLES)
    public ResponseEntity<ApiResponse<InventoryItemResponse>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInventoryItemRequest request) {

        return ApiResponseUtil.success("Part updated successfully.", service.updateItem(id, request));
    }

    @GetMapping("/items")
    @PreAuthorize(LOOKUP_ROLES)
    public ResponseEntity<ApiResponse<Page<InventoryItemResponse>>> listItems(
            @RequestParam(required = false) Long garageId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponseUtil.success(
                "Parts fetched successfully.",
                service.listItems(garageId, search, filter, includeInactive, page, size));
    }

    @GetMapping("/items/{id}")
    @PreAuthorize(LOOKUP_ROLES)
    public ResponseEntity<ApiResponse<InventoryItemResponse>> getItem(
            @PathVariable Long id,
            @RequestParam(required = false) Long garageId) {

        return ApiResponseUtil.success("Part fetched successfully.", service.getItem(id, garageId));
    }

    @PostMapping("/items/{id}/movements")
    @PreAuthorize(WRITE_ROLES)
    public ResponseEntity<ApiResponse<InventoryItemResponse>> recordMovement(
            @PathVariable Long id,
            @Valid @RequestBody StockMovementRequest request) {

        return ApiResponseUtil.created(
                "Stock movement recorded successfully.", service.recordMovement(id, request));
    }

    @GetMapping("/movements")
    @PreAuthorize(OVERSIGHT_ROLES)
    public ResponseEntity<ApiResponse<Page<StockMovementResponse>>> listMovements(
            @RequestParam(required = false) Long garageId,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) StockMovementType type,
            @RequestParam(required = false) String jobCardNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponseUtil.success(
                "Stock movements fetched successfully.",
                service.listMovements(garageId, itemId, type, jobCardNumber, page, size));
    }

    @GetMapping("/summary")
    @PreAuthorize(OVERSIGHT_ROLES)
    public ResponseEntity<ApiResponse<InventorySummaryResponse>> getSummary(
            @RequestParam(required = false) Long garageId) {

        return ApiResponseUtil.success("Inventory summary fetched successfully.", service.getSummary(garageId));
    }
}
