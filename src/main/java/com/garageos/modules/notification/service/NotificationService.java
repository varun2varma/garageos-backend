package com.garageos.modules.notification.service;

import com.garageos.modules.notification.dto.NotificationResponse;
import org.springframework.data.domain.Page;

public interface NotificationService {

    Page<NotificationResponse> list(Long userId, int page, int size);

    long unreadCount(Long userId);

    void markRead(Long userId, Long notificationId);

    int markAllRead(Long userId);
}
