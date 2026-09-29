package com.garageos.modules.media.storage;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * What a client needs to upload bytes directly to a storage provider,
 * without ever seeing that provider's credentials. Only meaningful for
 * providers that support direct client upload (R2); see
 * {@link MediaStorageProvider#createUploadAuthorization}.
 */
@Builder
public record UploadAuthorization(

        String uploadUrl,

        /** HTTP method the client must use against {@code uploadUrl} (e.g. "PUT"). */
        String method,

        /** Headers the client must send with the upload request (e.g. Content-Type). Never a credential. */
        Map<String, String> requiredHeaders,

        String storageKey,

        LocalDateTime expiresAt
) {
}
