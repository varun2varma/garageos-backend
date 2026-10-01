package com.garageos.modules.notification.worker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garageos.core.enums.notification.NotificationDeliveryStatus;
import com.garageos.core.enums.notification.NotificationOutboxStatus;
import com.garageos.core.enums.notification.NotificationPriority;
import com.garageos.modules.notification.config.NotificationProperties;
import com.garageos.modules.notification.entity.DeviceToken;
import com.garageos.modules.notification.entity.Notification;
import com.garageos.modules.notification.entity.NotificationDelivery;
import com.garageos.modules.notification.entity.NotificationOutbox;
import com.garageos.modules.notification.fcm.FcmSender;
import com.garageos.modules.notification.repository.DeviceTokenRepository;
import com.garageos.modules.notification.repository.NotificationDeliveryRepository;
import com.garageos.modules.notification.repository.NotificationOutboxRepository;
import com.garageos.modules.notification.repository.NotificationRepository;
import com.garageos.modules.notification.service.NotificationRecipient;
import com.garageos.modules.notification.service.NotificationRecipientResolver;
import com.garageos.modules.notification.service.NotificationTemplateFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transactional steps of the notification worker. Each public method is
 * one short database transaction; no method performs FCM I/O.
 *
 * Stage 1 (outbox -> notifications + delivery rows) runs in a single
 * transaction per outbox row, so a crash leaves nothing half-written and a
 * replay is safe. Stage 2 claims due deliveries with a lease, and the
 * actual FCM call happens in the worker outside any transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxProcessor {

    /** A claimed delivery is invisible to other claims for this long. */
    private static final long DELIVERY_LEASE_MINUTES = 2;
    private static final long MAX_BACKOFF_SECONDS = 3600;

    public record PendingSend(Long deliveryId, FcmSender.Message message) {
    }

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRecipientResolver recipientResolver;
    private final NotificationTemplateFactory templateFactory;
    private final NotificationProperties properties;
    private final FcmSender fcmSender;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---------------------------------------------------------------- outbox

    @Transactional
    public List<Long> claimOutboxBatch() {

        LocalDateTime now = LocalDateTime.now();

        int recovered = outboxRepository.recoverStaleLocks(
                now.minusMinutes(properties.getStaleLockMinutes()), now);
        if (recovered > 0) {
            log.warn("[NOTIF_OUTBOX] Recovered {} stale PROCESSING row(s).", recovered);
        }

        List<NotificationOutbox> batch = outboxRepository.lockDueBatch(now, properties.getBatchSize());

        List<Long> ids = new ArrayList<>();
        for (NotificationOutbox row : batch) {
            row.setStatus(NotificationOutboxStatus.PROCESSING);
            row.setLockedAt(now);
            row.setAttemptCount(row.getAttemptCount() + 1);
            ids.add(row.getId());
        }
        outboxRepository.saveAll(batch);
        return ids;
    }

    @Transactional
    public void fanOut(Long outboxId) {

        NotificationOutbox outbox = outboxRepository.findById(outboxId).orElse(null);
        if (outbox == null || outbox.getStatus() != NotificationOutboxStatus.PROCESSING) {
            return;
        }

        Map<String, Object> facts = parseFacts(outbox.getPayload());

        List<NotificationRecipient> recipients = recipientResolver.resolve(
                outbox.getEventType(), outbox.getGarageId(), facts);

        log.info("[NOTIF_RESOLVE] outboxId={} key={} garageId={} recipients={}",
                outbox.getId(), outbox.getEventKey(), outbox.getGarageId(), recipients.size());

        LocalDateTime now = LocalDateTime.now();
        boolean push = fcmSender.isAvailable();

        for (NotificationRecipient recipient : recipients) {

            NotificationTemplateFactory.Rendered text =
                    templateFactory.render(outbox.getEventType(), recipient.persona(), facts);

            Long notificationId = notificationRepository.insertIfAbsent(
                    recipient.userId(),
                    outbox.getGarageId(),
                    outbox.getEventType().name(),
                    outbox.getEventType().getCategory().name(),
                    outbox.getEventType().getPriority().name(),
                    text.title(),
                    text.message(),
                    outbox.getEntityType(),
                    outbox.getEntityId(),
                    outbox.getJobCardId(),
                    outbox.getEventKey(),
                    now);

            if (notificationId == null || !push) {
                continue;
            }

            for (DeviceToken token : deviceTokenRepository.findByUserIdAndActiveTrue(recipient.userId())) {
                deliveryRepository.insertIfAbsent(notificationId, token.getId(), now);
            }
        }

        outbox.setStatus(NotificationOutboxStatus.SENT);
        outbox.setProcessedAt(now);
        outbox.setLockedAt(null);
        outbox.setLastError(null);
        outboxRepository.save(outbox);
    }

    @Transactional
    public void recordOutboxFailure(Long outboxId, String error) {

        NotificationOutbox outbox = outboxRepository.findById(outboxId).orElse(null);
        if (outbox == null) {
            return;
        }

        outbox.setLockedAt(null);
        outbox.setLastError(truncate(error));

        if (outbox.getAttemptCount() >= properties.getMaxAttempts()) {
            outbox.setStatus(NotificationOutboxStatus.FAILED);
            log.error("[NOTIF_OUTBOX] Permanently failed outboxId={} key={} attempts={}",
                    outbox.getId(), outbox.getEventKey(), outbox.getAttemptCount());
        } else {
            outbox.setStatus(NotificationOutboxStatus.PENDING);
            outbox.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoffSeconds(outbox.getAttemptCount())));
            log.warn("[NOTIF_OUTBOX] Retry scheduled outboxId={} key={} attempt={}",
                    outbox.getId(), outbox.getEventKey(), outbox.getAttemptCount());
        }
        outboxRepository.save(outbox);
    }

    // ------------------------------------------------------------- deliveries

    @Transactional
    public List<PendingSend> claimDeliveries() {

        LocalDateTime now = LocalDateTime.now();

        List<NotificationDelivery> due = deliveryRepository.lockDueBatch(now, properties.getBatchSize());
        List<PendingSend> sends = new ArrayList<>();

        for (NotificationDelivery delivery : due) {

            DeviceToken token = deviceTokenRepository.findById(delivery.getDeviceTokenId()).orElse(null);
            Notification notification = notificationRepository.findById(delivery.getNotificationId()).orElse(null);

            if (token == null || notification == null || !Boolean.TRUE.equals(token.getActive())) {
                delivery.setStatus(NotificationDeliveryStatus.INVALID_TOKEN);
                delivery.setLastError("token inactive");
                continue;
            }

            // Lease: if this worker dies before recording the result, the
            // delivery becomes due again after the lease.
            delivery.setNextAttemptAt(now.plusMinutes(DELIVERY_LEASE_MINUTES));

            sends.add(new PendingSend(delivery.getId(), toMessage(token, notification)));
        }

        deliveryRepository.saveAll(due);
        return sends;
    }

    @Transactional
    public void recordDeliveryResult(Long deliveryId, FcmSender.Result result) {

        NotificationDelivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
        if (delivery == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);

        switch (result.status()) {

            case SUCCESS -> {
                delivery.setStatus(NotificationDeliveryStatus.SENT);
                delivery.setSentAt(now);
                delivery.setLastError(null);
                log.info("[NOTIF_FCM] SENT deliveryId={} notificationId={} deviceTokenId={}",
                        deliveryId, delivery.getNotificationId(), delivery.getDeviceTokenId());
            }

            case INVALID_TOKEN -> {
                delivery.setStatus(NotificationDeliveryStatus.INVALID_TOKEN);
                delivery.setLastError(truncate(result.detail()));
                deviceTokenRepository.findById(delivery.getDeviceTokenId()).ifPresent(t -> {
                    t.setActive(false);
                    deviceTokenRepository.save(t);
                });
                log.warn("[NOTIF_FCM] INVALID_TOKEN deliveryId={} deviceTokenId={} (token deactivated)",
                        deliveryId, delivery.getDeviceTokenId());
            }

            case TRANSIENT_ERROR, CONFIGURATION_ERROR -> {
                delivery.setLastError(truncate(result.detail()));
                if (delivery.getAttemptCount() >= properties.getMaxAttempts()) {
                    delivery.setStatus(NotificationDeliveryStatus.FAILED);
                    log.error("[NOTIF_FCM] FAILED deliveryId={} attempts={} reason={}",
                            deliveryId, delivery.getAttemptCount(), result.status());
                } else {
                    delivery.setStatus(NotificationDeliveryStatus.PENDING);
                    delivery.setNextAttemptAt(now.plusSeconds(backoffSeconds(delivery.getAttemptCount())));
                    log.warn("[NOTIF_FCM] RETRY deliveryId={} attempt={} reason={}",
                            deliveryId, delivery.getAttemptCount(), result.status());
                }
            }
        }

        deliveryRepository.save(delivery);
    }

    // ----------------------------------------------------------------- helpers

    private FcmSender.Message toMessage(DeviceToken token, Notification n) {

        Map<String, String> data = new HashMap<>();
        data.put("notificationId", String.valueOf(n.getId()));
        data.put("type", n.getType().name());
        putIfNotNull(data, "entityType", n.getEntityType());
        putIfNotNull(data, "entityId", n.getEntityId());
        putIfNotNull(data, "jobCardId", n.getJobCardId());
        putIfNotNull(data, "garageId", n.getGarageId());

        NotificationPriority priority = n.getPriority();
        String channel = switch (priority) {
            case HIGH -> "garagest_urgent";
            case LOW -> "garagest_low";
            default -> "garagest_default";
        };

        return new FcmSender.Message(
                token.getToken(),
                n.getTitle(),
                n.getMessage(),
                data,
                priority == NotificationPriority.HIGH,
                channel);
    }

    private static void putIfNotNull(Map<String, String> map, String key, Object value) {
        if (value != null) {
            map.put(key, String.valueOf(value));
        }
    }

    private Map<String, Object> parseFacts(String payload) {
        if (payload == null || payload.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Unreadable outbox payload", e);
        }
    }

    long backoffSeconds(int attempts) {
        long exponent = Math.max(0, Math.min(attempts - 1, 20));
        return Math.min(MAX_BACKOFF_SECONDS, properties.getBaseBackoffSeconds() << exponent);
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
