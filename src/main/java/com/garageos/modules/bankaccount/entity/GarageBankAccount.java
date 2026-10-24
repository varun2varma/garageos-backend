package com.garageos.modules.bankaccount.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.bankaccount.BankAccountType;
import com.garageos.core.enums.bankaccount.BankAccountVerificationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A garage's registered payout account. The number exists only as {@code accountNumberEnc} (never logged, never
 * returned) plus its last four digits for masked display. One row is {@code current} per garage; replacing the account
 * retires the old row instead of overwriting it.
 */
@Entity
@Table(name = "garage_bank_account")
@Getter
@Setter
@NoArgsConstructor
public class GarageBankAccount extends BaseEntity {

    @Column(name = "garage_id", nullable = false, updatable = false)
    private Long garageId;

    @Column(name = "account_holder_name", nullable = false, length = 100)
    private String accountHolderName;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(nullable = false, length = 11)
    private String ifsc;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 10)
    private BankAccountType accountType;

    @Column(name = "account_number_enc", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String accountNumberEnc;

    @Column(name = "account_number_last4", nullable = false, length = 4, updatable = false)
    private String accountNumberLast4;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private BankAccountVerificationStatus verificationStatus;

    @Column(name = "verification_provider", length = 40)
    private String verificationProvider;

    @Column(name = "verification_reference", length = 100)
    private String verificationReference;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "failure_reason", length = 300)
    private String failureReason;

    @Column(name = "consent_at", nullable = false, updatable = false)
    private LocalDateTime consentAt;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private Long createdByUserId;

    @Column(name = "superseded_at")
    private LocalDateTime supersededAt;

    @Column(name = "retired_by_user_id")
    private Long retiredByUserId;
}
