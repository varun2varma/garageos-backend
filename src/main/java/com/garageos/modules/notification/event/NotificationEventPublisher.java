package com.garageos.modules.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.notification.config.NotificationProperties;
import com.garageos.modules.notification.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes a notification event to the transactional outbox. Deliberately
 * participates in the caller's transaction (default REQUIRED, never
 * REQUIRES_NEW): the outbox row commits with the business change or rolls
 * back with it. Performs no recipient lookup and no network I/O.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationProperties properties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void publish(NotificationEvent event) {

        if (!properties.isEnabled()) {
            return;
        }

        if (event == null || event.getType() == null || event.getEntityId() == null) {
            log.warn("[NOTIF_OUTBOX] Ignoring malformed notification event.");
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>(event.getFacts());

        Long actorId = currentActorId();
        if (actorId != null) {
            payload.putIfAbsent(NotificationFacts.ACTOR_USER_ID, actorId);
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("[NOTIF_OUTBOX] Could not serialize facts for event {}; dropping notification.",
                    event.getEventKey());
            return;
        }

        Long id = outboxRepository.insertIfAbsent(
                event.getEventKey(),
                event.getType().name(),
                event.getGarageId(),
                event.getEntityType(),
                event.getEntityId(),
                event.getJobCardId(),
                json,
                LocalDateTime.now());

        if (id == null) {
            log.debug("[NOTIF_OUTBOX] Duplicate event ignored key={}", event.getEventKey());
        } else {
            log.debug("[NOTIF_OUTBOX] Queued outboxId={} key={} garageId={}",
                    id, event.getEventKey(), event.getGarageId());
        }
    }

    private Long currentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof GarageUserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }
}
