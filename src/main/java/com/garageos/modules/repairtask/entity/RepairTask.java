package com.garageos.modules.repairtask.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.RepairStatus;
import com.garageos.modules.complaint.entity.Complaint;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.jobassignment.entity.JobAssignment;
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
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimate_item_id")
    private EstimateItem estimateItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RepairStatus status = RepairStatus.PENDING;

    @Column(length = 100)
    private String technicianName;

    /**
     * Authoritative current technician ownership for this RepairTask,
     * additive to (never a replacement for) technicianName above, which
     * remains a display/history snapshot and is never read for
     * authorization. Nullable: not every existing/legacy RepairTask has
     * a linked JobAssignment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_assignment_id")
    private JobAssignment jobAssignment;

    private LocalDateTime assignedAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(length = 2000)
    private String remarks;
}