package com.garageos.core.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Application-level encryption for individual sensitive fields (bank account numbers). AES-256-GCM with a random IV per
 * value and the owning row bound in as associated data, so a ciphertext copied to another row will not decrypt.
 *
 * <p>The key comes only from the environment ({@code GARAGEST_FIELD_ENCRYPTION_KEY}, base64 of 32 random bytes). With no
 * key the encryptor reports {@link #isConfigured()} false and callers refuse the operation: there is never a plaintext
 * fallback. Values carry a {@code v1:} prefix so a future key rotation can tell generations apart.
 */
@Component
public class FieldEncryptor {

    private static final String PREFIX = "v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public FieldEncryptor(@Value("${garagest.field-encryption.key:}") String base64Key) {
        SecretKeySpec parsed = null;
        if (base64Key != null && !base64Key.isBlank()) {
            byte[] raw = Base64.getDecoder().decode(base64Key.trim());
            if (raw.length != 32) {
                throw new IllegalStateException(
                        "garagest.field-encryption.key must be the base64 encoding of exactly 32 bytes.");
            }
            parsed = new SecretKeySpec(raw, "AES");
        }
        this.key = parsed;
    }

    public boolean isConfigured() {
        return key != null;
    }

    /** @param aad what this value belongs to, for example {@code "bank:42"}; must be supplied again to decrypt */
    public String encrypt(String plain, String aad) {

        requireConfigured();

        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);

            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            c.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            return PREFIX + Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed.", e);
        }
    }

    public String decrypt(String stored, String aad) {

        requireConfigured();

        if (stored == null || !stored.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Unrecognised ciphertext.");
        }

        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_BYTES));
            c.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            return new String(c.doFinal(all, IV_BYTES, all.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Ciphertext could not be decrypted for this record.", e);
        }
    }

    private void requireConfigured() {
        if (key == null) {
            throw new IllegalStateException("Field encryption is not configured.");
        }
    }
}
