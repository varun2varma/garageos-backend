package com.garageos.modules.delivery.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.DeliveryStatus;
import com.garageos.modules.invoice.entity.Invoice;
import com.garageos.modules.jobcard.entity.JobCard;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "delivery")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Delivery extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_card_id", nullable = false, unique = true)
    JobCard jobCard;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false, unique = true)
    Invoice invoice;

    LocalDateTime deliveryDateTime;

    @Column(length = 100)
    String deliveredBy;

    @Column(length = 100)
    String receivedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    DeliveryStatus status;

    @Column(columnDefinition = "TEXT")
    String remarks;

}