package com.garageos.modules.identity.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

/**
 * Startup rules for the JWT signing secret. The secret must be supplied by the environment
 * ({@code SECURITY_JWT_SECRET}); the application refuses to start without a usable one.
 *
 * <ul>
 *   <li>present and not blank;</li>
 *   <li>at least {@value #MIN_BYTES} bytes (an HMAC-SHA256 key should be at least 256 bits);</li>
 *   <li>not a trivially repetitive value;</li>
 *   <li>not one of the values that were ever committed to the repository. Those are compared by SHA-256
 *       fingerprint only: the values themselves are not (and must never be) stored here, and the
 *       error message never repeats the configured value.</li>
 * </ul>
 */
public final class JwtSecretPolicy {

    public static final int MIN_BYTES = 32;

    static final int MIN_DISTINCT_CHARACTERS = 10;

    /**
     * SHA-256 fingerprints of signing secrets that were committed to Git at some point and must be treated as
     * public. The default that shipped in {@code application.properties} is the only one in the repository history.
     */
    static final Set<String> EXPOSED_FINGERPRINTS = Set.of(
            "9e6847f62b40f6ec7b05f1e1484389127a0ac111be052c6aa09156766b69f307");

    private JwtSecretPolicy() {
    }

    /** @throws IllegalStateException with a message that never contains the secret */
    public static void validate(String secret) {
        validate(secret, EXPOSED_FINGERPRINTS);
    }

    static void validate(String secret, Set<String> rejectedFingerprints) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT signing secret is not configured. Set the SECURITY_JWT_SECRET environment variable "
                            + "(at least " + MIN_BYTES + " random bytes) before starting the application.");
        }

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);

        if (bytes.length < MIN_BYTES) {
            throw new IllegalStateException(
                    "JWT signing secret is too short (" + bytes.length + " bytes; at least " + MIN_BYTES
                            + " are required). Set a long random SECURITY_JWT_SECRET.");
        }

        long distinct = secret.chars().distinct().count();

        if (distinct < MIN_DISTINCT_CHARACTERS) {
            throw new IllegalStateException(
                    "JWT signing secret is too repetitive to be random. Set a long random SECURITY_JWT_SECRET.");
        }

        if (rejectedFingerprints.contains(fingerprint(bytes))) {
            throw new IllegalStateException(
                    "JWT signing secret is a value that was previously committed to the repository and must not "
                            + "be used. Generate a new random SECURITY_JWT_SECRET and rotate it in the secret manager.");
        }
    }

    static String fingerprint(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** For tests and operators: the fingerprint of a candidate secret, never the secret itself. */
    public static String fingerprintOf(String secret) {
        return fingerprint(secret.getBytes(StandardCharsets.UTF_8));
    }
}
