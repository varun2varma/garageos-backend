package com.garageos.modules.audit.dto.response;

import com.garageos.core.enums.audit.AuditEventType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditEventResponse {

    private Long id;
    private AuditEventType eventType;
    private String entityType;
    private Long entityId;
    private Long actorId;
    private String actorRole;
    private String metadata;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
}
