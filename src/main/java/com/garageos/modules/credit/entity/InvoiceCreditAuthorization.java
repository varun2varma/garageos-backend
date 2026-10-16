package com.garageos.modules.credit.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.CreditAuthorizationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * An Owner's authorization to release a vehicle while its invoice is unpaid (V81). It is a financial
 * authorization only: it never changes the invoice's payment status and never unlocks the invoice PDF.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoice_credit_authorization")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceCreditAuthorization extends BaseEntity {

    @Column(name = "garage_id", nullable = false)
    Long garageId;

    @Column(name = "invoice_id", nullable = false)
    Long invoiceId;

    @Column(name = "job_card_id", nullable = false)
    Long jobCardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    CreditAuthorizationStatus status = CreditAuthorizationStatus.ACTIVE;

    @Column(name = "authorized_amount", nullable = false, precision = 12, scale = 2)
    BigDecimal authorizedAmount;

    @Column(name = "due_date", nullable = false)
    LocalDate dueDate;

    @Column(nullable = false, length = 500)
    String reason;

    @Column(name = "authorized_by_user_id", nullable = false)
    Long authorizedByUserId;

    @Column(name = "authorized_at", nullable = false)
    LocalDateTime authorizedAt;

    @Column(name = "revoked_by_user_id")
    Long revokedByUserId;

    @Column(name = "revoked_at")
    LocalDateTime revokedAt;

    @Column(name = "revoke_reason", length = 500)
    String revokeReason;

    @Column(name = "settled_at")
    LocalDateTime settledAt;

    @Version
    Long version;
}
