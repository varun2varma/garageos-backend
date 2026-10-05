package com.garageos.modules.inventory.repository;

import com.garageos.modules.inventory.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryItemRepository
        extends JpaRepository<InventoryItem, Long>, JpaSpecificationExecutor<InventoryItem> {

    Optional<InventoryItem> findByIdAndGarageId(Long id, Long garageId);

    /** Row-locks the item so concurrent movements serialise and the balance can never go negative. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.id = :id AND i.garageId = :garageId")
    Optional<InventoryItem> findByIdAndGarageIdForUpdate(
            @Param("id") Long id, @Param("garageId") Long garageId);

    boolean existsByGarageIdAndPartNumber(Long garageId, String partNumber);

    long countByGarageIdAndActiveTrue(Long garageId);

    @Query("""
            SELECT COUNT(i) FROM InventoryItem i
            WHERE i.garageId = :garageId AND i.active = true
              AND i.quantity > 0 AND i.quantity <= i.reorderLevel
            """)
    long countLowStock(@Param("garageId") Long garageId);

    @Query("""
            SELECT COUNT(i) FROM InventoryItem i
            WHERE i.garageId = :garageId AND i.active = true AND i.quantity <= 0
            """)
    long countOutOfStock(@Param("garageId") Long garageId);
}
