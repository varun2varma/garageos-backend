package com.garageos.modules.media.service;

import com.garageos.core.exception.MediaException;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * The one place that decides "is this Drive failure something reauthorizing
 * fixes, or something a backoff retry fixes" — used identically by the
 * synchronous first attempt (MediaServiceImpl.uploadMedia) and the
 * background retry (MediaUploadRetryServiceImpl), so both apply the exact
 * same rule instead of two hand-maintained copies drifting apart.
 */
public final class DriveFailureClassifier {

    private DriveFailureClassifier() {
    }

    public enum Classification {

        /** Stored authorization is invalid/revoked/missing. Never auto-retried. */
        AUTH,

        /** Timeout, 429, 5xx, or another transient network/Drive failure. Backoff-retried. */
        TRANSIENT
    }

    public static Classification classify(Throwable ex) {

        if (ex instanceof MediaException mediaEx
                && mediaEx.getErrorCode()
                        == MediaException.MediaErrorCode.MEDIA_DRIVE_AUTH_REQUIRED) {
            return Classification.AUTH;
        }

        if (ex instanceof TokenResponseException) {
            return Classification.AUTH;
        }

        if (ex instanceof GoogleJsonResponseException googleEx) {

            int status = googleEx.getStatusCode();

            if (status == 401 || status == 403) {
                return Classification.AUTH;
            }

            // 429 and 5xx are exactly the transient cases the task calls
            // out; anything else 4xx-shaped from Drive is still treated as
            // transient here rather than terminal — a bad request against
            // Drive itself (e.g. a malformed folder id) would keep failing
            // identically on every backoff retry and simply exhaust into
            // FAILED, which is the correct terminal outcome anyway.
            return Classification.TRANSIENT;
        }

        if (ex instanceof SocketTimeoutException) {
            return Classification.TRANSIENT;
        }

        if (ex instanceof IOException) {
            return Classification.TRANSIENT;
        }

        // Unclassified (e.g. a GeneralSecurityException building the Drive
        // client). Not an auth-token problem specifically, so treat as
        // transient rather than assuming reauthorization would help.
        return Classification.TRANSIENT;
    }

    /** Sanitized (no token/URL/request metadata) message safe to persist and return to a client. */
    public static String sanitizedMessage(Throwable ex) {

        Classification classification = classify(ex);

        if (classification == Classification.AUTH) {
            return "Google Drive rejected the stored authorization. Reauthorization is required.";
        }

        return "Google Drive could not complete this operation. It will be retried automatically.";
    }
}
