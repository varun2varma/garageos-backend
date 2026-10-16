package com.garageos.modules.jobcard.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.modules.complaint.entity.Complaint;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@org.hibernate.annotations.BatchSize(size = 50)
@Table(name = "job_card")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JobCard extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "garage_id",
            nullable = false
    )
    Garage garage;

    @Column(nullable = false, unique = true, length = 30)
    String jobCardNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    Vehicle vehicle;

    @Column(nullable = false)
    LocalDate serviceDate;

    /**
     * Who opened this job card (V82), recorded at creation from the authenticated principal only. Null on
     * records created before attribution existed - those are NEVER back-filled by guessing.
     */
    @Column(name = "created_by_user_id")
    Long createdByUserId;

    /** Comma-separated roles the creator held when creating it (snapshot), e.g. {@code SERVICE_ADVISOR}. */
    @Column(name = "created_by_roles", length = 100)
    String createdByRoles;

    /** Name of the creator at creation time (snapshot; survives renames / deactivation). */
    @Column(name = "created_by_name", length = 200)
    String createdByName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    JobCardStatus status;

    @Column(nullable = false)
    Long odometerReading;

    LocalDate estimatedDeliveryDate;

    @OneToMany(
            mappedBy = "jobCard",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<Complaint> complaints = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    String remarks;

    /**
     * Nullable link back to the Booking this JobCard originated from, when
     * applicable (V37). Not required - most JobCards, including every
     * historical one, have no booking. Not yet populated by any create
     * flow; see the Booking module's completion report for why this is
     * schema-only in this sprint (deliberately not wired into
     * JobCardServiceImpl.createJob to avoid touching that method's
     * currently in-flight behavior in the same change).
     */
    @Column(name = "booking_id")
    Long bookingId;
}