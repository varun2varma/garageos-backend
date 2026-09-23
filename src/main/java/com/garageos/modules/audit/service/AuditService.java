package com.garageos.modules.audit.service;

import com.garageos.core.enums.audit.AuditEventType;
import com.garageos.modules.audit.dto.response.AuditEventResponse;

import java.util.List;
import java.util.Map;

public interface AuditService {

    void record(
            AuditEventType eventType,
            String entityType,
            Long entityId,
            Long garageId,
            Map<String, Object> metadata
    );

    void record(
            AuditEventType eventType,
            String entityType,
            Long entityId,
            Long garageId,
            Map<String, Object> metadata,
            Double latitude,
            Double longitude
    );

    /**
     * The audit trail for one entity, restricted to the caller's own
     * garage - an event recorded under a different garage than the
     * caller's is silently excluded rather than causing a 403, matching
     * this codebase's existing not-found-over-forbidden convention. A
     * CUSTOMER-role caller (no garageId) always gets an empty list: this
     * read is an operational/staff view, not a customer-facing one.
     */
    List<AuditEventResponse> findForEntity(String entityType, Long entityId);
}
