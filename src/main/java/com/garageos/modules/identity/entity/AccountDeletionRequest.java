package com.garageos.modules.identity.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.identity.AccountDeletionRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Google Play "external account deletion" requirement: an anonymous visitor
 * to the public /delete-account page submits an email/mobile identifier,
 * and — if it matches a real account — a request row is recorded here.
 * This table only ever records that a request was made; it never triggers
 * the actual destructive AuthServiceImpl.deleteAccount() operation, which
 * still requires the account owner to be authenticated. Fulfilling a
 * REQUESTED row (verifying ownership and calling deleteAccount() on the
 * matched userId) is a deliberately separate, not-yet-built step — see
 * AuthServiceImpl.requestAccountDeletion's own doc comment.
 */
@Entity
@Table(name = "account_deletion_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDeletionRequest extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountDeletionRequestStatus status = AccountDeletionRequestStatus.REQUESTED;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

}
