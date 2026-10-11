package com.garageos.modules.catalog.entity;

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
import java.time.LocalDate;

/** One effective-dated GST rate for a rule code (V69). No rates are seeded. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "gst_tax_rule")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GstTaxRule extends BaseEntity {

    @Column(name = "rule_code", nullable = false, length = 40)
    String ruleCode;

    @Column(length = 255)
    String description;

    @Column(name = "rate_percent", nullable = false, precision = 5, scale = 2)
    BigDecimal ratePercent;

    @Column(name = "effective_from", nullable = false)
    LocalDate effectiveFrom;

    @Column(name = "effective_to")
    LocalDate effectiveTo;

    @Column(nullable = false)
    boolean active = true;

    /** Where the rate was confirmed (for example a notification number or portal reference). */
    @Column(name = "source_reference", length = 255)
    String sourceReference;

    @Column(name = "verified_on")
    LocalDate verifiedOn;

    @Column(name = "created_by_user_id")
    Long createdByUserId;
}
