package com.garageos.modules.invoice.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.InvoiceStatus;
import com.garageos.core.enums.PaymentStatus;
import com.garageos.modules.estimate.entity.Estimate;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoice")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Invoice extends BaseEntity {

    @Column(nullable = false, unique = true)
    String invoiceNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimate_id", nullable = false, unique = true)
    Estimate estimate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    InvoiceStatus invoiceStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    PaymentStatus paymentStatus;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal discount;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal gst;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal grandTotal;

    @Column(columnDefinition = "TEXT")
    String remarks;
}