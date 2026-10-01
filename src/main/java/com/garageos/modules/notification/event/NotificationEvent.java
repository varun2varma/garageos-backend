package com.garageos.modules.notification.event;

import com.garageos.core.enums.notification.NotificationEventType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A business event worth notifying about. Carries only ids and small
 * facts needed to resolve recipients and render text later, in the worker.
 *
 * The event key identifies one logical transition instance:
 * TYPE:ENTITY_TYPE:ENTITY_ID:discriminator. The discriminator tells apart
 * separate passes through the same state (e.g. a job card re-entering
 * REPAIR_COMPLETED after a failed quality check).
 */
public final class NotificationEvent {

    private final NotificationEventType type;
    private final Long garageId;
    private final String entityType;
    private final Long entityId;
    private final Long jobCardId;
    private final String discriminator;
    private final Map<String, Object> facts = new LinkedHashMap<>();

    private NotificationEvent(NotificationEventType type, Long garageId, Long entityId,
                              Long jobCardId, String discriminator) {
        this.type = type;
        this.garageId = garageId;
        this.entityType = type.getEntityType();
        this.entityId = entityId;
        this.jobCardId = jobCardId;
        this.discriminator = discriminator;
    }

    public static NotificationEvent of(NotificationEventType type, Long garageId, Long entityId,
                                       Long jobCardId, Object discriminator) {
        return new NotificationEvent(type, garageId, entityId, jobCardId, String.valueOf(discriminator));
    }

    public NotificationEvent fact(String key, Object value) {
        if (value != null) {
            facts.put(key, value);
        }
        return this;
    }

    public NotificationEvent technicianUserIds(Collection<Long> userIds) {
        if (userIds != null && !userIds.isEmpty()) {
            facts.put(NotificationFacts.TECHNICIAN_USER_IDS, new ArrayList<>(userIds));
        }
        return this;
    }

    public NotificationEventType getType() {
        return type;
    }

    public Long getGarageId() {
        return garageId;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public Long getJobCardId() {
        return jobCardId;
    }

    public Map<String, Object> getFacts() {
        return facts;
    }

    public String getEventKey() {
        return type.name() + ":" + entityType + ":" + entityId + ":" + discriminator;
    }
}
