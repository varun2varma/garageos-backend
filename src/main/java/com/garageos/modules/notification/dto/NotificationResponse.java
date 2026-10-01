package com.garageos.modules.notification.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String type,
        String category,
        String priority,
        String title,
        String message,
        String entityType,
        Long entityId,
        Long jobCardId,
        Long garageId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt) {
}
