package com.garageos.modules.audit.entity;

import com.garageos.core.enums.audit.AuditEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Indexes are created by V48's migration directly (idx_audit_event_entity,
// idx_audit_event_garage, idx_audit_event_type) - not redeclared here,
// since ddl-auto=validate never lets Hibernate create schema from
// annotations anyway (see this repo's own CLAUDE.md §5).
@Entity
@Table(name = "audit_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private AuditEventType eventType;

    /** e.g. "Booking", "NavigationTrip", "VehicleHandover", "NavigationTripMedia". */
    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /** Null only for an event with no garage context yet resolvable. */
    @Column(name = "garage_id")
    private Long garageId;

    /** Null for a system-triggered event with no authenticated actor. */
    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_role")
    private String actorRole;

    /** Small JSON blob of event-specific detail — free-form by design. */
    @Column(name = "metadata")
    private String metadata;

    private Double latitude;
    private Double longitude;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
