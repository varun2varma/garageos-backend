package com.garageos.modules.notification.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.notification.NotificationCategory;
import com.garageos.core.enums.notification.NotificationEventType;
import com.garageos.core.enums.notification.NotificationPriority;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** In-app notification history row for exactly one recipient user. */
@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "garage_id")
    private Long garageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private NotificationEventType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationPriority priority;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "entity_type", length = 30)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "job_card_id")
    private Long jobCardId;

    @Column(name = "event_key", nullable = false, length = 255)
    private String eventKey;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
