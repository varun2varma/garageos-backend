package com.garageos.modules.media.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Employee/technician/owner-side media listing response — everything
 * {@link JobCardMediaResponse} has, plus audit/evidence metadata (WHO/WHEN/
 * WHERE it was captured, and upload timing). Deliberately a SEPARATE class
 * from {@link JobCardMediaResponse}, not an extension with extra fields
 * bolted on: {@link JobCardMediaResponse} is also used by the
 * customer-portal listing endpoint (CustomerPortalServiceImpl.getJobCardMedia),
 * and customers must never receive capture location or an employee's name —
 * see this feature's own privacy decision (garage/internal users get richer
 * audit information; customer-facing display does not, by construction of
 * using a different DTO/mapper method entirely, not a runtime visibility
 * check that could be forgotten).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobCardMediaAuditResponse {

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
    private String uploadStatus;
    private Integer retryCount;
    private String lastError;
    private String storageProvider;
    private Integer durationSeconds;

    // Audit/evidence metadata (V59) — see JobCardMedia's own doc comments
    // for exactly how each is sourced and why it's immutable.
    private LocalDateTime capturedAt;
    private String capturedByNameSnapshot;
    private Double latitude;
    private Double longitude;
    private Double locationAccuracyMeters;
    private String locationName;
    private LocalDateTime uploadedAt;
    private String uploadedByNameSnapshot;

    /** Whether a generated "evidence" display variant exists yet (never the raw object key). */
    private boolean hasEvidenceImage;
}
