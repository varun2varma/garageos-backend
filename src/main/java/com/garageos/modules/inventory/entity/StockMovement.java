package com.garageos.modules.inventory.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.inventory.StockMovementType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/** Immutable ledger entry of a stock change (V68). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventory_stock_movement")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockMovement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    InventoryItem item;

    @Column(name = "garage_id", nullable = false)
    Long garageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    StockMovementType type;

    /** Signed delta actually applied to the item quantity. */
    @Column(nullable = false, precision = 12, scale = 3)
    BigDecimal quantity;

    @Column(name = "balance_after", nullable = false, precision = 12, scale = 3)
    BigDecimal balanceAfter;

    @Column(length = 255)
    String reason;

    /** Set when stock is issued against a job card (consumption). */
    @Column(name = "job_card_id")
    Long jobCardId;

    @Column(name = "performed_by_user_id")
    Long performedByUserId;
}
