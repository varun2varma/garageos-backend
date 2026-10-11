package com.garageos.modules.estimateitem.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.EstimateItemType;
import com.garageos.core.enums.catalog.EntrySource;
import com.garageos.core.enums.catalog.ReviewStatus;
import com.garageos.modules.complaint.entity.Complaint;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.inspectionfinding.entity.InspectionFinding;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "estimate_item")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EstimateItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimate_id", nullable = false)
    Estimate estimate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id")
    Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_finding_id")
    InspectionFinding inspectionFinding;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    EstimateItemType itemType;

    @Column(nullable = false)
    String description;

    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal totalPrice;

    /**
     * Customer selection state (Mission: item-level select/deselect
     * before approval). A deselected item is excluded from
     * subtotal/GST/grandTotal recalculation (EstimateItemServiceImpl)
     * and from repair-task creation on approval
     * (RepairTaskServiceImpl.createRepairTasks) - so "does not continue
     * to repair/invoice" falls out naturally from the same total the
     * invoice is generated from, rather than a second exclusion list.
     * Defaults true so every existing item stays selected.
     */
    @Column(nullable = false)
    Boolean selected = true;

    /** Real FK to catalog_item for MASTER lines; null for MANUAL and LEGACY lines (V69). */
    @Column(name = "catalog_item_id")
    Long catalogItemId;

    /** Provenance garage, always derived server-side from the job card. */
    @Column(name = "garage_id")
    Long garageId;

    @Column(length = 20)
    String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_source", nullable = false, length = 20)
    EntrySource entrySource = EntrySource.LEGACY;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    ReviewStatus reviewStatus = ReviewStatus.NOT_REQUIRED;

    /** Authenticated creator, never taken from the request. */
    @Column(name = "created_by_user_id")
    Long createdByUserId;

    @Column(name = "manual_note", length = 500)
    String manualNote;

    // Classification snapshot copied from the catalog at selection time; later
    // catalog edits never rewrite these.
    @Column(name = "hsn_sac_code", length = 10)
    String hsnSacCode;

    @Column(name = "tax_rule_code", length = 40)
    String taxRuleCode;

    @Column(name = "gst_rate_percent", precision = 5, scale = 2)
    BigDecimal gstRatePercent;
}