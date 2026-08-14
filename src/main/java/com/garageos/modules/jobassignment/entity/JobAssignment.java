package com.garageos.modules.jobassignment.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.JobAssignmentStatus;
import com.garageos.core.enums.JobAssignmentType;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.jobcard.entity.JobCard;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "job_assignments")
public class JobAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garage_id", nullable = false)
    private Garage garage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_card_id", nullable = false)
    private JobCard jobCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimate_item_id")
    private EstimateItem estimateItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobAssignmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 30)
    private JobAssignmentType assignmentType;

    @Column(nullable = false)
    private LocalDateTime assignedAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private Double estimatedHours;

    private Double actualHours;

    @Column(length = 1000)
    private String remarks;

}