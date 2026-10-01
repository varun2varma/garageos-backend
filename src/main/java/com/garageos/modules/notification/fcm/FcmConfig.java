package com.garageos.modules.notification.fcm;

import com.garageos.modules.notification.config.NotificationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chooses the FCM sender. The application always starts: with push
 * disabled, or credentials missing/invalid, a no-op sender is used and
 * only in-app notification history is produced.
 */
@Slf4j
@Configuration
public class FcmConfig {

    @Bean
    public FcmSender fcmSender(NotificationProperties properties) {

        if (!properties.isEnabled()) {
            log.info("[NOTIF_FCM] Notifications disabled (notifications.enabled=false).");
            return new NoopFcmSender();
        }

        if (!properties.hasCredentials()) {
            log.warn("[NOTIF_FCM] Notifications enabled but FIREBASE_CREDENTIALS_BASE64 is not set; "
                    + "push delivery is off, in-app history only.");
            return new NoopFcmSender();
        }

        try {
            FcmSender sender = new FirebaseFcmSender(properties.getCredentialsBase64());
            log.info("[NOTIF_FCM] Firebase sender initialised.");
            return sender;
        } catch (Exception e) {
            log.error("[NOTIF_FCM] Could not initialise Firebase ({}); push delivery is off.",
                    e.getClass().getSimpleName());
            return new NoopFcmSender();
        }
    }
}
