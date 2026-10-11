-- V72: in-app chat scoped to a booking or a job card.
-- One conversation per (scope_type, scope_id). Participants are NOT stored: they are resolved from the live
-- booking / job-card / trip / assignment relationships at every request, so a reassigned driver or technician loses
-- access immediately and a newly assigned one gains it. Messages are never edited or deleted by this feature.
CREATE TABLE chat_conversation (
    id BIGSERIAL PRIMARY KEY,
    scope_type VARCHAR(20) NOT NULL,
    scope_id BIGINT NOT NULL,
    garage_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_chat_scope_type CHECK (scope_type IN ('BOOKING', 'JOB_CARD')),
    CONSTRAINT uk_chat_scope UNIQUE (scope_type, scope_id)
);

CREATE TABLE chat_message (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES chat_conversation (id),
    sender_user_id BIGINT NOT NULL,
    sender_role VARCHAR(30) NOT NULL,
    sender_name VARCHAR(200),
    body VARCHAR(2000) NOT NULL,
    -- Client-generated id: resending the same message after a dropped response returns the stored row.
    client_message_id VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_message_conversation ON chat_message (conversation_id, id DESC);

CREATE UNIQUE INDEX uk_chat_message_client
    ON chat_message (conversation_id, sender_user_id, client_message_id)
    WHERE client_message_id IS NOT NULL;

CREATE TABLE chat_read_state (
    conversation_id BIGINT NOT NULL REFERENCES chat_conversation (id),
    user_id BIGINT NOT NULL,
    last_read_message_id BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (conversation_id, user_id)
);
