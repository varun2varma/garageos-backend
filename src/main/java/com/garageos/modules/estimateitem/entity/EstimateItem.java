package com.garageos.modules.estimateitem.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.EstimateItemType;
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
}