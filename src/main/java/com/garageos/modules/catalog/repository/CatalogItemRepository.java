package com.garageos.modules.catalog.repository;

import com.garageos.core.enums.catalog.CatalogItemType;
import com.garageos.core.enums.catalog.ClassificationStatus;
import com.garageos.modules.catalog.entity.CatalogItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CatalogItemRepository
        extends JpaRepository<CatalogItem, Long>, JpaSpecificationExecutor<CatalogItem> {

    long countByTaxRuleCodeAndClassificationStatus(String taxRuleCode, ClassificationStatus status);

    Optional<CatalogItem> findByItemTypeAndNameNormalized(CatalogItemType itemType, String nameNormalized);

    Page<CatalogItem> findByItemTypeAndActiveTrueOrderByNameAsc(CatalogItemType itemType, Pageable pageable);

    /** {@code pattern} is a pre-built, wildcard-safe LIKE pattern over the normalised name. */
    @Query("""
            SELECT c FROM CatalogItem c
            WHERE c.itemType = :type AND c.active = true
              AND c.nameNormalized LIKE :pattern
            ORDER BY c.name ASC
            """)
    Page<CatalogItem> search(
            @Param("type") CatalogItemType type,
            @Param("pattern") String pattern,
            Pageable pageable);
}
