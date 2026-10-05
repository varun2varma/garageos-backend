package com.garageos.modules.payment.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.payment.PaymentMethod;
import com.garageos.core.enums.payment.PaymentRecordStatus;
import com.garageos.modules.invoice.entity.Invoice;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One recorded payment against an invoice (V67). Invoice.paymentStatus is derived from these. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    Invoice invoice;

    /** Denormalised from invoice -> estimate -> job card so every query can be garage-scoped cheaply. */
    @Column(name = "garage_id", nullable = false)
    Long garageId;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    PaymentMethod method;

    @Column(length = 100)
    String reference;

    @Column(name = "received_by_user_id")
    Long receivedByUserId;

    @Column(name = "paid_at", nullable = false)
    LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    PaymentRecordStatus status;

    @Column(name = "idempotency_key", length = 80)
    String idempotencyKey;
}
