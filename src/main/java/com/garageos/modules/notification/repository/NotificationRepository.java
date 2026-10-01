package com.garageos.modules.notification.repository;

import com.garageos.modules.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("""
            UPDATE Notification n
               SET n.isRead = true, n.readAt = :now, n.updatedAt = :now
             WHERE n.userId = :userId AND n.isRead = false
            """)
    int markAllRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Idempotent insert: returns the new id, or null when a row for the
     * same (event_key, user_id) already exists.
     */
    @Query(value = """
            INSERT INTO notification
                (user_id, garage_id, type, category, priority, title, message,
                 entity_type, entity_id, job_card_id, event_key, is_read, created_at)
            VALUES
                (:userId, :garageId, :type, :category, :priority, :title, :message,
                 :entityType, :entityId, :jobCardId, :eventKey, FALSE, :now)
            ON CONFLICT (event_key, user_id) DO NOTHING
            RETURNING id
            """, nativeQuery = true)
    Long insertIfAbsent(
            @Param("userId") Long userId,
            @Param("garageId") Long garageId,
            @Param("type") String type,
            @Param("category") String category,
            @Param("priority") String priority,
            @Param("title") String title,
            @Param("message") String message,
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("jobCardId") Long jobCardId,
            @Param("eventKey") String eventKey,
            @Param("now") LocalDateTime now);
}
