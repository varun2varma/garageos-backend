-- GarageOS - Audit Module
-- Foundation audit trail (Mission requirement). Previously nothing in
-- this codebase recorded who did what, when, to what prior value - every
-- module's event/ package directory existed but was empty, and BaseEntity
-- only gives created_at/updated_at with no actor. This table is the first
-- real audit log.

CREATE TABLE audit_event (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    garage_id BIGINT NULL,
    actor_id BIGINT NULL,
    actor_role VARCHAR(30) NULL,
    metadata TEXT NULL,
    latitude DOUBLE PRECISION NULL,
    longitude DOUBLE PRECISION NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_event_entity ON audit_event (entity_type, entity_id, created_at);
CREATE INDEX idx_audit_event_garage ON audit_event (garage_id, created_at);
CREATE INDEX idx_audit_event_type ON audit_event (event_type);
