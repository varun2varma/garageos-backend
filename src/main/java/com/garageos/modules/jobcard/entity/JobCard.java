package com.garageos.modules.jobcard.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "job_card")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JobCard extends BaseEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    JobCardStatus status;

    @Column(nullable = false)
    Long odometerReading;

    LocalDate estimatedDeliveryDate;

    @Column(columnDefinition = "TEXT")
    String remarks;
}