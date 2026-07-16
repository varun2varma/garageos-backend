package com.garageos.modules.inspection.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.InspectionStatus;
import com.garageos.modules.complaint.entity.Complaint;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inspection")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Inspection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    Complaint complaint;

    @Column(columnDefinition = "TEXT")
    String inspectionNotes;

    @Column(columnDefinition = "TEXT")
    String recommendedWork;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    InspectionStatus status;

}