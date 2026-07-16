package com.garageos.modules.estimate.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.EstimateStatus;
import com.garageos.modules.jobcard.entity.JobCard;
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
@Table(name = "estimate")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Estimate extends BaseEntity {

    @Column(nullable = false, unique = true)
    String estimateNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_card_id", nullable = false)
    JobCard jobCard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    EstimateStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal discount;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal gst;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal grandTotal;

    @Column(columnDefinition = "TEXT")
    String remarks;
}