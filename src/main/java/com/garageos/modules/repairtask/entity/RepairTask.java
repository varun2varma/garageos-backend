package com.garageos.modules.repairtask.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.RepairStatus;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.jobcard.entity.JobCard;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "repair_task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepairTask extends BaseEntity {

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "job_card_id",
            nullable = false
    )
    private JobCard jobCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "estimate_item_id",
            nullable = false
    )
    private EstimateItem estimateItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RepairStatus status = RepairStatus.PENDING;

    @Column(length = 100)
    private String technicianName;

    private LocalDateTime assignedAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(length = 2000)
    private String remarks;
}