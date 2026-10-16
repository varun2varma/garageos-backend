package com.garageos.modules.credit.dto;

import com.garageos.core.enums.CreditAuthorizationStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class CreditAuthorizationResponse {

    private Long id;
    private Long invoiceId;
    private String invoiceNumber;
    private Long jobCardId;
    private String jobCardNumber;
    private CreditAuthorizationStatus status;
    private BigDecimal authorizedAmount;

    /** Invoice grand total minus recorded payments - computed from the payment ledger, never stored. */
    private BigDecimal outstandingAmount;

    private LocalDate dueDate;

    /** True while ACTIVE and past its due date. */
    private boolean overdue;

    private String reason;
    private Long authorizedByUserId;
    private LocalDateTime authorizedAt;
    private LocalDateTime revokedAt;
    private String revokeReason;
    private LocalDateTime settledAt;
}
