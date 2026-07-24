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

    @Column(length = 100)
    private String inspectedBy;

    private LocalDateTime inspectedAt;

    @Column(length = 2000)
    private String remarks;
}