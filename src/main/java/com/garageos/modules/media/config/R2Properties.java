package com.garageos.modules.media.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cloudflare R2 configuration, entirely environment-driven (see
 * application.properties). Every field defaults to empty so the application
 * context starts cleanly before R2 is provisioned — {@link #isConfigured()}
 * is checked at call time by {@code R2MediaStorageProvider}, never at
 * startup. No credential is ever logged (see that class's own doc comment).
 */
@Getter
@Component
public class R2Properties {

    @Value("${r2.account-id:}")
    private String accountId;

    @Value("${r2.access-key-id:}")
    private String accessKeyId;

    @Value("${r2.secret-access-key:}")
    private String secretAccessKey;

    @Value("${r2.bucket-name:}")
    private String bucketName;

    @Value("${r2.upload-url-ttl-minutes:15}")
    private long uploadUrlTtlMinutes;

    @Value("${r2.playback-url-ttl-minutes:10}")
    private long playbackUrlTtlMinutes;

    public boolean isConfigured() {
        return notBlank(accountId)
                && notBlank(accessKeyId)
                && notBlank(secretAccessKey)
                && notBlank(bucketName);
    }

    /** Which environment variable(s) are still missing — for a clear, non-secret-leaking error message. */
    public String missingVariableName() {
        if (!notBlank(accountId)) return "R2_ACCOUNT_ID";
        if (!notBlank(accessKeyId)) return "R2_ACCESS_KEY_ID";
        if (!notBlank(secretAccessKey)) return "R2_SECRET_ACCESS_KEY";
        if (!notBlank(bucketName)) return "R2_BUCKET_NAME";
        return null;
    }

    public String endpoint() {
        return "https://" + accountId + ".r2.cloudflarestorage.com";
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
