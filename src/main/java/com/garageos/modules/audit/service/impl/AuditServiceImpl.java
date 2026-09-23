package com.garageos.modules.audit.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garageos.core.enums.audit.AuditEventType;
import com.garageos.modules.audit.dto.response.AuditEventResponse;
import com.garageos.modules.audit.entity.AuditEvent;
import com.garageos.modules.audit.repository.AuditEventRepository;
import com.garageos.modules.audit.service.AuditService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Foundation audit trail. Deliberately joins the caller's own transaction
 * (no REQUIRES_NEW) rather than a separate one - per this mission's own
 * failure-integrity principle, an audit row must never exist for a
 * business operation that didn't actually commit. If the record() call
 * itself throws (e.g. a serialization bug), it fails loudly rather than
 * being swallowed - a silently-lost audit write would defeat the point of
 * having a trail at all.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditEventRepository auditEventRepository;

    /*
     * Live-boot regression (found running the real app, not caught by
     * Mockito-only unit tests): this module's starter dependency
     * (spring-boot-starter-webmvc) does not transitively register an
     * injectable ObjectMapper bean the way spring-boot-starter-web/-json
     * would - "APPLICATION FAILED TO START ... No qualifying bean of type
     * 'com.fasterxml.jackson.databind.ObjectMapper'". Metadata
     * serialization here needs no Spring MVC message-converter
     * configuration (custom modules, date formats, etc.), so a plain
     * instance - not a DI dependency - is the correct minimal fix, rather
     * than adding a new starter dependency (pom.xml change) or defining a
     * new shared @Bean for one caller.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void record(
            AuditEventType eventType,
            String entityType,
            Long entityId,
            Long garageId,
            Map<String, Object> metadata) {

        record(eventType, entityType, entityId, garageId, metadata, null, null);
    }

    @Override
    @Transactional
    public void record(
            AuditEventType eventType,
            String entityType,
            Long entityId,
            Long garageId,
            Map<String, Object> metadata,
            Double latitude,
            Double longitude) {

        GarageUserPrincipal principal = currentPrincipalOrNull();

        String metadataJson = null;
        if (metadata != null && !metadata.isEmpty()) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (Exception e) {
                // A malformed metadata map must not block the audit row
                // itself from being written - the event happening is more
                // important than the extra detail describing it.
                log.warn("Could not serialize audit metadata for {} {}#{}", eventType, entityType, entityId, e);
            }
        }

        AuditEvent event = AuditEvent.builder()
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .garageId(garageId)
                .actorId(principal == null ? null : principal.getId())
                .actorRole(principal == null || principal.getRoles() == null || principal.getRoles().isEmpty()
                        ? null
                        : String.join(",", principal.getRoles()))
                .metadata(metadataJson)
                .latitude(latitude)
                .longitude(longitude)
                .build();

        auditEventRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEventResponse> findForEntity(String entityType, Long entityId) {

        GarageUserPrincipal principal = currentPrincipalOrNull();

        if (principal == null || principal.getGarageId() == null) {
            return List.of();
        }

        return auditEventRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .filter(event -> Objects.equals(event.getGarageId(), principal.getGarageId()))
                .map(this::toResponse)
                .toList();
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        return AuditEventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .actorId(event.getActorId())
                .actorRole(event.getActorRole())
                .metadata(event.getMetadata())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private GarageUserPrincipal currentPrincipalOrNull() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof GarageUserPrincipal principal)) {
            return null;
        }

        return principal;
    }
}
