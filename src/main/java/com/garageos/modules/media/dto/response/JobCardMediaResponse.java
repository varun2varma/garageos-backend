package com.garageos.modules.media.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Media-listing response shape for both the employee/technician/owner
 * listing endpoint and the customer-portal listing endpoint.
 *
 * Deliberately excludes {@code driveFileId}/{@code driveWebViewLink} — those
 * are internal storage references, not something a client needs (or, per
 * the current Google Drive sharing configuration, could even use directly).
 * Clients fetch actual bytes through the authenticated
 * "/media/{mediaId}/content" endpoint instead.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobCardMediaResponse {

    private Long id;

    private Long jobCardId;

    private Long repairTaskId;

    private String mediaType;

    private String mediaStage;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private Long uploadedBy;

    private String visibility;

    private LocalDateTime createdAt;
}
