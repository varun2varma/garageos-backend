package com.garageos.modules.notification.repository;

import com.garageos.modules.notification.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    List<NotificationDelivery> findByNotificationId(Long notificationId);

    /** Idempotent insert; returns null when the (notification, device) pair exists. */
    @Query(value = """
            INSERT INTO notification_delivery
                (notification_id, device_token_id, status, attempt_count, next_attempt_at, created_at)
            VALUES
                (:notificationId, :deviceTokenId, 'PENDING', 0, :now, :now)
            ON CONFLICT (notification_id, device_token_id) DO NOTHING
            RETURNING id
            """, nativeQuery = true)
    Long insertIfAbsent(
            @Param("notificationId") Long notificationId,
            @Param("deviceTokenId") Long deviceTokenId,
            @Param("now") LocalDateTime now);

    @Query(value = """
            SELECT * FROM notification_delivery
             WHERE status = 'PENDING' AND next_attempt_at <= :now
             ORDER BY id
             LIMIT :limit
             FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<NotificationDelivery> lockDueBatch(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
