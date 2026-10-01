package com.garageos.modules.notification.fcm;

import java.util.Map;

/**
 * Delivery boundary to FCM. Implementations must never throw for delivery
 * problems - they classify them in the returned {@link Result}.
 */
public interface FcmSender {

    enum Status {
        SUCCESS,
        /** Provider/network problem worth retrying. */
        TRANSIENT_ERROR,
        /** Token is unregistered/invalid: deactivate it, never retry it. */
        INVALID_TOKEN,
        /** Credential / project configuration problem. */
        CONFIGURATION_ERROR
    }

    record Result(Status status, String detail) {
        public static Result success() {
            return new Result(Status.SUCCESS, null);
        }
    }

    record Message(
            String token,
            String title,
            String body,
            Map<String, String> data,
            boolean highPriority,
            String androidChannelId) {
    }

    /** False when push is disabled or credentials are missing. */
    boolean isAvailable();

    Result send(Message message);
}
