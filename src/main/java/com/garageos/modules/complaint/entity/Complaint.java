package com.garageos.modules.complaint.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.ComplaintStatus;
import com.garageos.modules.jobcard.entity.JobCard;
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
@Table(name = "complaint")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Complaint extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_card_id", nullable = false)
    JobCard jobCard;

    @Column(nullable = false, columnDefinition = "TEXT")
    String complaint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ComplaintStatus status;
}