package com.garageos.modules.notification.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.util.Base64;

/**
 * FCM HTTP v1 sender using the Firebase Admin SDK. Credentials come only
 * from the base64 environment value; the raw token is never logged.
 */
@Slf4j
public class FirebaseFcmSender implements FcmSender {

    private static final String APP_NAME = "garagest-notifications";

    private final FirebaseMessaging messaging;

    public FirebaseFcmSender(String credentialsBase64) throws Exception {

        byte[] json = Base64.getDecoder().decode(credentialsBase64.trim());

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(json)))
                .setConnectTimeout(10_000)
                .setReadTimeout(15_000)
                .build();

        FirebaseApp app = FirebaseApp.getApps().stream()
                .filter(a -> APP_NAME.equals(a.getName()))
                .findFirst()
                .orElseGet(() -> FirebaseApp.initializeApp(options, APP_NAME));

        this.messaging = FirebaseMessaging.getInstance(app);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Result send(Message message) {

        try {

            AndroidNotification.Builder androidNotification = AndroidNotification.builder();
            if (message.androidChannelId() != null) {
                androidNotification.setChannelId(message.androidChannelId());
            }

            com.google.firebase.messaging.Message.Builder builder =
                    com.google.firebase.messaging.Message.builder()
                            .setToken(message.token())
                            .setNotification(Notification.builder()
                                    .setTitle(message.title())
                                    .setBody(message.body())
                                    .build())
                            .setAndroidConfig(AndroidConfig.builder()
                                    .setPriority(message.highPriority()
                                            ? AndroidConfig.Priority.HIGH
                                            : AndroidConfig.Priority.NORMAL)
                                    .setNotification(androidNotification.build())
                                    .build());

            if (message.data() != null) {
                builder.putAllData(message.data());
            }

            messaging.send(builder.build());
            return Result.success();

        } catch (FirebaseMessagingException e) {
            return classify(e);
        } catch (Exception e) {
            return new Result(Status.TRANSIENT_ERROR, e.getClass().getSimpleName());
        }
    }

    private Result classify(FirebaseMessagingException e) {

        MessagingErrorCode code = e.getMessagingErrorCode();

        if (code == MessagingErrorCode.UNREGISTERED
                || code == MessagingErrorCode.SENDER_ID_MISMATCH) {
            return new Result(Status.INVALID_TOKEN, String.valueOf(code));
        }

        if (code == MessagingErrorCode.THIRD_PARTY_AUTH_ERROR
                || e.getErrorCode() == ErrorCode.UNAUTHENTICATED
                || e.getErrorCode() == ErrorCode.PERMISSION_DENIED) {
            return new Result(Status.CONFIGURATION_ERROR, String.valueOf(e.getErrorCode()));
        }

        return new Result(Status.TRANSIENT_ERROR,
                code != null ? String.valueOf(code) : String.valueOf(e.getErrorCode()));
    }
}
