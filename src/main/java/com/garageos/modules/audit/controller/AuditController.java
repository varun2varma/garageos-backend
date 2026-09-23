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

    @GetMapping("/events")
    public List<AuditEventResponse> findForEntity(
            @RequestParam String entityType,
            @RequestParam Long entityId) {

        return auditService.findForEntity(entityType, entityId);
    }
}
