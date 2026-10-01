-- GarageOS - Notification System
-- Additive only: no existing table is altered.
--
--   device_token           one row per FCM registration token (an install, not an identity)
--   notification           in-app notification history per recipient user (source of truth)
--   notification_outbox    transactional outbox, written in the business transaction
--   notification_delivery  per-device push delivery state for a notification

CREATE TABLE device_token (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    app_version VARCHAR(50) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_device_token_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE UNIQUE INDEX uk_device_token_token ON device_token (token);
CREATE INDEX idx_device_token_user_active ON device_token (user_id) WHERE is_active = TRUE;

CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    garage_id BIGINT NULL,
    type VARCHAR(60) NOT NULL,
    category VARCHAR(30) NOT NULL,
    priority VARCHAR(10) NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(500) NOT NULL,
    entity_type VARCHAR(30) NULL,
    entity_id BIGINT NULL,
    job_card_id BIGINT NULL,
    event_key VARCHAR(255) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Idempotency: one logical notification per (event, recipient).
CREATE UNIQUE INDEX uk_notification_event_user ON notification (event_key, user_id);
CREATE INDEX idx_notification_user_created ON notification (user_id, created_at DESC, id DESC);
CREATE INDEX idx_notification_user_unread ON notification (user_id) WHERE is_read = FALSE;
CREATE INDEX idx_notification_job_card ON notification (job_card_id);
CREATE INDEX idx_notification_garage_created ON notification (garage_id, created_at);

CREATE TABLE notification_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_key VARCHAR(255) NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    garage_id BIGINT NULL,
    entity_type VARCHAR(30) NULL,
    entity_id BIGINT NULL,
    job_card_id BIGINT NULL,
    payload TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT now(),
    locked_at TIMESTAMP NULL,
    last_error VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NULL,
    processed_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX uk_notification_outbox_event_key ON notification_outbox (event_key);
CREATE INDEX idx_notification_outbox_status_next ON notification_outbox (status, next_attempt_at);

CREATE TABLE notification_delivery (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    device_token_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT now(),
    sent_at TIMESTAMP NULL,
    last_error VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_notification_delivery_notification FOREIGN KEY (notification_id) REFERENCES notification (id),
    CONSTRAINT fk_notification_delivery_device FOREIGN KEY (device_token_id) REFERENCES device_token (id)
);

CREATE UNIQUE INDEX uk_notification_delivery_pair ON notification_delivery (notification_id, device_token_id);
CREATE INDEX idx_notification_delivery_status_next ON notification_delivery (status, next_attempt_at);
