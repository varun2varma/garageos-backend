package com.garageos.modules.notification.fcm;

/** Used when notifications are disabled or Firebase credentials are absent. */
public class NoopFcmSender implements FcmSender {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Result send(Message message) {
        return new Result(Status.CONFIGURATION_ERROR, "FCM is not configured");
    }
}
