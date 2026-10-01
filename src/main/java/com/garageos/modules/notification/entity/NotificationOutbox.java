package com.garageos.modules.notification.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.notification.NotificationEventType;
import com.garageos.core.enums.notification.NotificationOutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Transactional outbox row; written in the business transaction. */
@Entity
@Table(name = "notification_outbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationOutbox extends BaseEntity {

    @Column(name = "event_key", nullable = false, length = 255)
    private String eventKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 60)
    private NotificationEventType eventType;

    @Column(name = "garage_id")
    private Long garageId;

    @Column(name = "entity_type", length = 30)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "job_card_id")
    private Long jobCardId;

    /** Small JSON object of recipient-resolution facts (ids only). */
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationOutboxStatus status = NotificationOutboxStatus.PENDING;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
