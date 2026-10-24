-- Emergency Assist: a roadside-assistance request raised before login by a customer whose mobile number was proven
-- with an OTP. A dedicated table (not booking): a booking requires a registered vehicle and a future time, an
-- emergency has neither, and forcing it into booking would loosen that table's integrity.
-- Additive: nothing existing is altered.

CREATE TABLE emergency_assist_request (
    id                   BIGSERIAL PRIMARY KEY,
    request_code         VARCHAR(20)   NOT NULL,
    idempotency_key      VARCHAR(80)   NOT NULL,
    status               VARCHAR(20)   NOT NULL,
    assistance_type      VARCHAR(30)   NOT NULL,
    garage_id            BIGINT        NOT NULL,
    customer_id          BIGINT        NOT NULL,
    contact_name         VARCHAR(100)  NOT NULL,
    contact_mobile       VARCHAR(15)   NOT NULL,
    contact_email        VARCHAR(150),
    vehicle_registration VARCHAR(30),
    vehicle_description  VARCHAR(200),
    latitude             NUMERIC(10,7) NOT NULL,
    longitude            NUMERIC(10,7) NOT NULL,
    address              VARCHAR(500),
    notes                VARCHAR(1000),
    garage_remarks       VARCHAR(1000),
    decided_by_user_id   BIGINT,
    decided_at           TIMESTAMP,
    expires_at           TIMESTAMP     NOT NULL,
    created_at           TIMESTAMP     NOT NULL,
    updated_at           TIMESTAMP,

    CONSTRAINT fk_emergency_garage   FOREIGN KEY (garage_id)   REFERENCES garage(id),
    CONSTRAINT fk_emergency_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT uk_emergency_code UNIQUE (request_code),
    -- The same submission retried (same key, same number) is the same request, never a second one.
    CONSTRAINT uk_emergency_idempotency UNIQUE (contact_mobile, idempotency_key),
    CONSTRAINT ck_emergency_lat CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_emergency_lng CHECK (longitude BETWEEN -180 AND 180)
);

-- At most one live request per verified number: concurrent submissions collide here, not just in application code.
CREATE UNIQUE INDEX uk_emergency_one_active_per_mobile
    ON emergency_assist_request (contact_mobile) WHERE status IN ('REQUESTED', 'ACCEPTED');

CREATE INDEX idx_emergency_garage_status ON emergency_assist_request (garage_id, status);
CREATE INDEX idx_emergency_expiry ON emergency_assist_request (status, expires_at);
