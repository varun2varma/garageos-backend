package com.garageos.modules.finalreport.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.report.FinalReportAudience;
import com.garageos.core.enums.report.FinalReportEmailStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * One issued final-report document. Rows are immutable as documents: a
 * regeneration inserts the next version, and only the e-mail delivery
 * bookkeeping columns are ever updated on an existing row.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "job_card_final_report")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JobCardFinalReport extends BaseEntity {

    @Column(name = "job_card_id", nullable = false)
    Long jobCardId;

    @Column(name = "garage_id", nullable = false)
    Long garageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    FinalReportAudience audience;

    @Column(nullable = false)
    int version;

    @Column(name = "file_name", nullable = false, length = 255)
    String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    String contentType = "application/pdf";

    @Column(name = "file_size", nullable = false)
    long fileSize;

    @Column(nullable = false)
    byte[] content;

    @Column(name = "generated_by")
    Long generatedBy;

    @Column(name = "generated_at", nullable = false)
    LocalDateTime generatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_status", nullable = false, length = 20)
    FinalReportEmailStatus emailStatus = FinalReportEmailStatus.PENDING;

    @Column(name = "email_attempts", nullable = false)
    int emailAttempts;

    @Column(name = "email_recipient", length = 150)
    String emailRecipient;

    @Column(name = "email_sent_at")
    LocalDateTime emailSentAt;

    @Column(name = "email_next_attempt_at")
    LocalDateTime emailNextAttemptAt;

    @Column(name = "email_last_error", length = 500)
    String emailLastError;
}
