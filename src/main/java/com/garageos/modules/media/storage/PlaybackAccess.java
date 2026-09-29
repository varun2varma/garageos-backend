package com.garageos.modules.media.storage;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * How a client should fetch one media item's bytes for playback, without
 * ever seeing a storage provider's credentials.
 *
 * Two shapes, depending on {@code direct}:
 *   - direct=true  (R2): {@code url} is a short-lived, provider-signed GET
 *     URL the client fetches directly — GarageST's backend is not in this
 *     byte path at all.
 *   - direct=false (GOOGLE_DRIVE, legacy): {@code url} is GarageST's own
 *     existing "/content" endpoint path, which the client must call with
 *     its normal bearer token (via {@code headers}) — Drive has no clean
 *     direct-to-client access model in this codebase, so this path
 *     necessarily still proxies through Spring Boot (see
 *     GoogleDriveMediaStorageProvider's own doc comment).
 */
@Builder
public record PlaybackAccess(

        String url,

        boolean direct,

        Map<String, String> headers,

        LocalDateTime expiresAt,

        /**
         * False only for a requested-but-not-yet-generated thumbnail on a
         * video (never generated in this pass — see
         * MediaProcessingScheduler's own doc comment). When false, {@code url}
         * is null and the caller should show a placeholder rather than ever
         * requesting the (potentially huge) original just to populate a grid
         * tile.
         */
        boolean available
) {
    public static PlaybackAccess unavailable() {
        return new PlaybackAccess(null, false, Map.of(), null, false);
    }
}
