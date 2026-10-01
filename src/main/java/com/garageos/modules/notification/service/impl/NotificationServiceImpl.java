package com.garageos.modules.notification.service.impl;

import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.notification.dto.NotificationResponse;
import com.garageos.modules.notification.entity.Notification;
import com.garageos.modules.notification.repository.NotificationRepository;
import com.garageos.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    static final int MAX_PAGE_SIZE = 50;

    private final NotificationRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(Long userId, int page, int size) {

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        return repository
                .findByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(safePage, safeSize))
                .map(NotificationServiceImpl::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long notificationId) {

        // Scoped to the owner: another user's id is indistinguishable from a missing one.
        Notification notification = repository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found with id : " + notificationId));

        if (Boolean.TRUE.equals(notification.getIsRead())) {
            return;
        }

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        repository.save(notification);
    }

    @Override
    @Transactional
    public int markAllRead(Long userId) {
        return repository.markAllRead(userId, LocalDateTime.now());
    }

    private static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType().name(),
                n.getCategory().name(),
                n.getPriority().name(),
                n.getTitle(),
                n.getMessage(),
                n.getEntityType(),
                n.getEntityId(),
                n.getJobCardId(),
                n.getGarageId(),
                Boolean.TRUE.equals(n.getIsRead()),
                n.getReadAt(),
                n.getCreatedAt());
    }
}
