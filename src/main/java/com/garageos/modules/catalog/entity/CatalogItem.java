package com.garageos.modules.catalog.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.catalog.CatalogItemType;
import com.garageos.core.enums.catalog.ClassificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Approved, garage-independent identity of a part or service (V69).
 * Holds NO price, stock or garage data: those stay on the garage's
 * inventory item and on each estimate line.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "catalog_item")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CatalogItem extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    CatalogItemType itemType;

    @Column(nullable = false, length = 150)
    String name;

    @Column(name = "name_normalized", nullable = false, length = 150)
    String nameNormalized;

    @Column(length = 500)
    String description;

    @Column(name = "default_unit", nullable = false, length = 20)
    String defaultUnit;

    /** HSN for a PART, SAC for a SERVICE; null while unclassified. */
    @Column(name = "hsn_sac_code", length = 10)
    String hsnSacCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification_status", nullable = false, length = 20)
    ClassificationStatus classificationStatus = ClassificationStatus.UNCLASSIFIED;

    /** Logical reference to {@link GstTaxRule#getRuleCode()}; rates are effective-dated. */
    @Column(name = "tax_rule_code", length = 40)
    String taxRuleCode;

    @Column(nullable = false)
    boolean active = true;

    @Column(name = "created_by_user_id")
    Long createdByUserId;

    @Column(name = "updated_by_user_id")
    Long updatedByUserId;

    /** Free-text grouping used for browsing and filtering, for example "Filters". */
    @Column(length = 60)
    String category;

    /** Where the classification came from (for example a notification or portal reference). Required to verify. */
    @Column(name = "classification_source", length = 255)
    String classificationSource;

    @Column(name = "classification_verified_at")
    java.time.LocalDateTime classificationVerifiedAt;

    @Column(name = "classification_verified_by_user_id")
    Long classificationVerifiedByUserId;

    /** Optimistic lock: two administrators cannot silently overwrite each other. */
    @Version
    @Column(nullable = false)
    Long version;
}
