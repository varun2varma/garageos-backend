package com.garageos.modules.qualitycheck.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.QualityCheckStatus;
import com.garageos.modules.jobcard.entity.JobCard;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "quality_check")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityCheck extends BaseEntity {

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "job_card_id",
            nullable = false,
            unique = true
    )
    private JobCard jobCard;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private QualityCheckStatus status = QualityCheckStatus.PENDING;

    /** Name snapshot of the inspector (kept for reports; legacy rows only have this). */
    @Column(length = 100)
    private String inspectedBy;

    /** Stable reference to the inspecting employee (V82); null on legacy rows that only recorded a name. */
    @Column(name = "inspected_by_user_id")
    private Long inspectedByUserId;

    private LocalDateTime inspectedAt;

    @Column(length = 2000)
    private String remarks;
}