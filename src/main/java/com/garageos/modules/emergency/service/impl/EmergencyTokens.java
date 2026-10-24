package com.garageos.modules.emergency.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Stateless, server-signed tokens for the Emergency Assist flow (no shared storage needed across instances):
 *
 * <ul>
 *   <li><b>Verification token</b> - issued only after the OTP for a number was verified; carries that number and an
 *       expiry, signed with HMAC. The client cannot forge or alter it, so a verified-contact flag is never trusted
 *       from the request body.</li>
 *   <li><b>Status token</b> - derived from the request code and the number, so an idempotent retry can return the
 *       same token and nothing secret has to be stored.</li>
 * </ul>
 * The signing key is derived from the JWT secret with a distinct prefix per token type.
 */
@Component
public class EmergencyTokens {

    static final long VERIFICATION_TTL_SECONDS = 15 * 60;

    private static final char[] CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();

    private final byte[] verificationKey;
    private final byte[] statusKey;
    private final SecureRandom random = new SecureRandom();

    public EmergencyTokens(@Value("${security.jwt.secret:}") String jwtSecret) {
        String secret = jwtSecret == null ? "" : jwtSecret;
        this.verificationKey = ("emergency-verification:" + secret).getBytes(StandardCharsets.UTF_8);
        this.statusKey = ("emergency-status:" + secret).getBytes(StandardCharsets.UTF_8);
    }

    public String issueVerification(String mobile) {
        return issueVerification(mobile, System.currentTimeMillis());
    }

    String issueVerification(String mobile, long nowMillis) {
        String payload = mobile + "|" + (nowMillis / 1000 + VERIFICATION_TTL_SECONDS);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encoded + "." + sign(verificationKey, encoded);
    }

    /** The verified mobile number the token proves, or empty when the token is missing, forged or expired. */
    public Optional<String> verifiedMobile(String token) {
        return verifiedMobile(token, System.currentTimeMillis());
    }

    Optional<String> verifiedMobile(String token, long nowMillis) {

        if (token == null || token.length() > 300) {
            return Optional.empty();
        }

        int dot = token.indexOf('.');
        if (dot <= 0) {
            return Optional.empty();
        }

        String encoded = token.substring(0, dot);

        if (!MessageDigest.isEqual(
                sign(verificationKey, encoded).getBytes(StandardCharsets.UTF_8),
                token.substring(dot + 1).getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            int bar = payload.indexOf('|');
            long expiresAt = Long.parseLong(payload.substring(bar + 1));
            if (nowMillis / 1000 > expiresAt) {
                return Optional.empty();
            }
            return Optional.of(payload.substring(0, bar));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    public String statusToken(String requestCode, String mobile) {
        return sign(statusKey, requestCode + ":" + mobile);
    }

    public boolean statusTokenMatches(String requestCode, String mobile, String supplied) {
        return supplied != null && MessageDigest.isEqual(
                statusToken(requestCode, mobile).getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }

    /** A short public reference such as {@code EA-7K3QX9MD}. Not a secret; unguessable status access needs the token. */
    public String newRequestCode() {
        StringBuilder sb = new StringBuilder("EA-");
        for (int i = 0; i < 8; i++) {
            sb.append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]);
        }
        return sb.toString();
    }

    private static String sign(byte[] key, String payload) {
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
