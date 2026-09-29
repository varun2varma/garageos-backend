package com.garageos.modules.media.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response for {@code GET /media/{mediaId}/access} — works uniformly for
 * both storage providers. For R2 media, {@code direct} is true and
 * {@code url} is a short-lived presigned URL the client fetches straight
 * from Cloudflare. For legacy Google Drive media, {@code direct} is false
 * and {@code url} is GarageST's own existing "/content" endpoint path (the
 * client must call it with its normal bearer token, via {@code headers}).
 */
@Getter
@Setter
@Builder
public class MediaAccessResponse {

    private Long mediaId;

    private String url;

    private boolean direct;

    private Map<String, String> headers;

    private LocalDateTime expiresAt;

    /**
     * False only for a requested thumbnail that doesn't exist (always the
     * case for video today — thumbnail generation isn't implemented for
     * video, see MediaProcessingScheduler). When false, {@code url} is null;
     * the client should show a placeholder rather than requesting the
     * original.
     */
    private boolean available;
}
