package com.garageos.modules.notification.repository;

import com.garageos.modules.notification.entity.NotificationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    /**
     * Idempotent insert in the caller's transaction: returns the new id, or
     * null when the event_key was already published.
     */
    @Query(value = """
            INSERT INTO notification_outbox
                (event_key, event_type, garage_id, entity_type, entity_id, job_card_id,
                 payload, status, attempt_count, next_attempt_at, created_at)
            VALUES
                (:eventKey, :eventType, :garageId, :entityType, :entityId, :jobCardId,
                 :payload, 'PENDING', 0, :now, :now)
            ON CONFLICT (event_key) DO NOTHING
            RETURNING id
            """, nativeQuery = true)
    Long insertIfAbsent(
            @Param("eventKey") String eventKey,
            @Param("eventType") String eventType,
            @Param("garageId") Long garageId,
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("jobCardId") Long jobCardId,
            @Param("payload") String payload,
            @Param("now") LocalDateTime now);

    /** Locks a bounded batch of due rows; concurrent workers skip locked rows. */
    @Query(value = """
            SELECT * FROM notification_outbox
             WHERE status = 'PENDING' AND next_attempt_at <= :now
             ORDER BY id
             LIMIT :limit
             FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<NotificationOutbox> lockDueBatch(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /** Puts rows abandoned in PROCESSING by a crashed worker back to PENDING. */
    @Modifying
    @Query("""
            UPDATE NotificationOutbox o
               SET o.status = com.garageos.core.enums.notification.NotificationOutboxStatus.PENDING,
                   o.lockedAt = null,
                   o.updatedAt = :now
             WHERE o.status = com.garageos.core.enums.notification.NotificationOutboxStatus.PROCESSING
               AND o.lockedAt < :staleBefore
            """)
    int recoverStaleLocks(@Param("staleBefore") LocalDateTime staleBefore, @Param("now") LocalDateTime now);
}
