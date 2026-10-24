package com.garageos.modules.media.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Protects the Google Drive authorization flow, whose two routes must stay reachable from a plain browser (no Bearer
 * header): {@code /authorize} starts it, {@code /callback} is where Google redirects.
 *
 * <ul>
 *   <li>{@code /authorize} only works with the operator token (property {@code google.drive.oauth.operator-token},
 *       env {@code GOOGLE_DRIVE_OAUTH_OPERATOR_TOKEN}). With no token configured the flow is disabled, so an
 *       anonymous caller can never start it.</li>
 *   <li>The URL it produces carries a signed, expiring {@code state}. {@code /callback} refuses any request without a
 *       valid state, so a stranger cannot complete a consent with their own Google account and have the backend store
 *       their Drive credentials in place of the operator's.</li>
 * </ul>
 * The state is an HMAC (keyed from the JWT secret), so it needs no shared storage across instances.
 */
@Component
public class DriveOAuthGuard {

    static final long STATE_TTL_MILLIS = 10 * 60 * 1000L;

    private final String operatorToken;
    private final byte[] key;

    public DriveOAuthGuard(
            @Value("${google.drive.oauth.operator-token:}") String operatorToken,
            @Value("${security.jwt.secret:}") String jwtSecret) {
        this.operatorToken = operatorToken == null ? "" : operatorToken;
        this.key = ("drive-oauth-state:" + (jwtSecret == null ? "" : jwtSecret)).getBytes(StandardCharsets.UTF_8);
    }

    /** True only when a token is configured AND the supplied one matches (constant-time). */
    public boolean operatorAllowed(String supplied) {
        if (operatorToken.isBlank() || supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(
                operatorToken.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }

    public String newState() {
        return newState(System.currentTimeMillis());
    }

    String newState(long issuedAtMillis) {
        String ts = Long.toString(issuedAtMillis);
        return ts + "." + sign(ts);
    }

    public boolean validState(String state) {
        return validState(state, System.currentTimeMillis());
    }

    boolean validState(String state, long nowMillis) {
        if (state == null) {
            return false;
        }
        int dot = state.indexOf('.');
        if (dot <= 0) {
            return false;
        }
        String ts = state.substring(0, dot);
        long issued;
        try {
            issued = Long.parseLong(ts);
        } catch (NumberFormatException e) {
            return false;
        }
        if (nowMillis - issued > STATE_TTL_MILLIS || issued - nowMillis > 60_000L) {
            return false;
        }
        return MessageDigest.isEqual(
                sign(ts).getBytes(StandardCharsets.UTF_8), state.substring(dot + 1).getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
