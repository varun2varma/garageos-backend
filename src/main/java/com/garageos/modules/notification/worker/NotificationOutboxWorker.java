package com.garageos.modules.notification.worker;

import com.garageos.modules.notification.config.NotificationProperties;
import com.garageos.modules.notification.fcm.FcmSender;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Polls the PostgreSQL outbox and delivers pending pushes.
 *
 * Runs on its own single-thread executor (not Spring's shared scheduler),
 * so a slow FCM call can never delay MediaUploadRetryScheduler or any other
 * scheduled job. Idle and cost-free while notifications.enabled=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxWorker {

    private final NotificationOutboxProcessor processor;
    private final NotificationProperties properties;
    private final FcmSender fcmSender;

    private ScheduledExecutorService executor;

    @PostConstruct
    void start() {

        if (!properties.isEnabled()) {
            return;
        }

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "notification-outbox-worker");
            t.setDaemon(true);
            return t;
        });

        executor.scheduleWithFixedDelay(
                this::runSafely,
                properties.getWorkerDelayMs(),
                properties.getWorkerDelayMs(),
                TimeUnit.MILLISECONDS);

        log.info("[NOTIF_OUTBOX] Worker started (delay={}ms, batch={}).",
                properties.getWorkerDelayMs(), properties.getBatchSize());
    }

    @PreDestroy
    void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void runSafely() {
        try {
            runOnce();
        } catch (Exception e) {
            // Never let an exception cancel the periodic task.
            log.error("[NOTIF_OUTBOX] Worker tick failed: {}", e.getClass().getSimpleName(), e);
        }
    }

    /** One worker tick; package-visible so tests can drive it directly. */
    void runOnce() {

        // Stage 1: outbox -> notification rows + delivery rows.
        for (Long outboxId : processor.claimOutboxBatch()) {
            try {
                processor.fanOut(outboxId);
            } catch (Exception e) {
                log.warn("[NOTIF_OUTBOX] Processing failed outboxId={} reason={}",
                        outboxId, e.getClass().getSimpleName());
                processor.recordOutboxFailure(outboxId, e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        // Stage 2: push delivery, only when FCM is actually available.
        if (!fcmSender.isAvailable()) {
            return;
        }

        List<NotificationOutboxProcessor.PendingSend> sends = processor.claimDeliveries();

        for (NotificationOutboxProcessor.PendingSend send : sends) {

            FcmSender.Result result = fcmSender.send(send.message());
            processor.recordDeliveryResult(send.deliveryId(), result);

            if (result.status() == FcmSender.Status.CONFIGURATION_ERROR) {
                // Credential/project problem: stop hammering FCM this tick.
                log.error("[NOTIF_FCM] Configuration error from FCM; pausing this tick. detail={}",
                        result.detail());
                break;
            }
        }
    }
}
