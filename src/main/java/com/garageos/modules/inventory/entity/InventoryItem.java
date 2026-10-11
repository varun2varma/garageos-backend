package com.garageos.modules.inventory.entity;

import com.garageos.core.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/** A stocked part of one garage (V68). Quantity is only ever changed through a StockMovement. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventory_item")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryItem extends BaseEntity {

    @Column(name = "garage_id", nullable = false)
    Long garageId;

    @Column(nullable = false, length = 150)
    String name;

    @Column(name = "part_number", length = 60)
    String partNumber;

    @Column(nullable = false, length = 20)
    String unit;

    @Column(nullable = false, precision = 12, scale = 3)
    BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "reorder_level", nullable = false, precision = 12, scale = 3)
    BigDecimal reorderLevel = BigDecimal.ZERO;

    @Column(nullable = false)
    boolean active = true;

    /** Optional link to the central catalog; stock and prices stay garage-specific (V69). */
    @Column(name = "catalog_item_id")
    Long catalogItemId;

    public boolean isOutOfStock() {
        return quantity.signum() <= 0;
    }

    /** In stock, but at or below the reorder level. */
    public boolean isLowStock() {
        return quantity.signum() > 0 && quantity.compareTo(reorderLevel) <= 0;
    }
}
