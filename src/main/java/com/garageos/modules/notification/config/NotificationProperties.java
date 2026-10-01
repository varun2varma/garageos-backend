package com.garageos.modules.notification.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Notification feature configuration. Everything is read from environment
 * variables via application.properties placeholders; nothing secret is
 * ever defaulted or committed.
 */
@Getter
@Component
public class NotificationProperties {

    /** Master switch (NOTIFICATIONS_ENABLED, default false). */
    private final boolean enabled;

    /** Base64 of the Firebase service-account JSON (FIREBASE_CREDENTIALS_BASE64). */
    private final String credentialsBase64;

    private final int batchSize;
    private final long workerDelayMs;
    private final int maxAttempts;
    private final long staleLockMinutes;
    private final long baseBackoffSeconds;

    public NotificationProperties(
            @Value("${notifications.enabled:false}") boolean enabled,
            @Value("${notifications.fcm.credentials-base64:}") String credentialsBase64,
            @Value("${notifications.worker.batch-size:50}") int batchSize,
            @Value("${notifications.worker.delay-ms:5000}") long workerDelayMs,
            @Value("${notifications.worker.max-attempts:6}") int maxAttempts,
            @Value("${notifications.worker.stale-lock-minutes:5}") long staleLockMinutes,
            @Value("${notifications.worker.base-backoff-seconds:30}") long baseBackoffSeconds) {
        this.enabled = enabled;
        this.credentialsBase64 = credentialsBase64;
        this.batchSize = batchSize;
        this.workerDelayMs = workerDelayMs;
        this.maxAttempts = maxAttempts;
        this.staleLockMinutes = staleLockMinutes;
        this.baseBackoffSeconds = baseBackoffSeconds;
    }

    public boolean hasCredentials() {
        return credentialsBase64 != null && !credentialsBase64.isBlank();
    }
}
