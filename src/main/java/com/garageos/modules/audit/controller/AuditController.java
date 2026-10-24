package com.garageos.modules.audit.controller;

import com.garageos.modules.audit.dto.response.AuditEventResponse;
import com.garageos.modules.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Operational/staff audit-trail read. Garage-scoped inside
 * AuditServiceImpl.findForEntity (an event recorded under a different
 * garage is silently excluded, and a caller with no garageId - e.g. a
 * customer - always gets an empty list) - not a full searchable audit UI,
 * which is a reasonable follow-up once this foundation is in place.
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /**
     * Garage staff only (the service additionally scopes to the caller's own garage and hides platform-only
     * actions). A platform administrator reads the audit trail through GET /api/v1/platform/audit.
     */
    @org.springframework.security.access.prepost.PreAuthorize(
            "hasAnyRole('OWNER', 'MANAGER', 'SERVICE_ADVISOR', 'ACCOUNTANT')")
    @GetMapping("/events")
    public List<AuditEventResponse> findForEntity(
            @RequestParam String entityType,
            @RequestParam Long entityId) {

        return auditService.findForEntity(entityType, entityId);
    }
}
