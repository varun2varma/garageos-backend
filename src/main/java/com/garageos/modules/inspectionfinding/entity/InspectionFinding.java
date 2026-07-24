package com.garageos.modules.inspectionfinding.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.InspectionFindingStatus;
import com.garageos.modules.complaint.entity.Complaint;
import com.garageos.modules.inspectionmaster.entity.InspectionMasterItem;
import com.garageos.modules.jobcard.entity.JobCard;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inspection_finding")
@Getter
@Setter
@NoArgsConstructor
public class InspectionFinding extends BaseEntity {

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_card_id", nullable = false)
    private JobCard jobCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_master_item_id", nullable = false)
    private InspectionMasterItem inspectionMasterItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id")
    private Complaint complaint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InspectionFindingStatus status;

    @Column(length = 2000)
    private String remarks;

}