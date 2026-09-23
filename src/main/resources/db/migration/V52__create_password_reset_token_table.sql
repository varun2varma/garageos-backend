-- GarageOS - Identity Module
-- Mission backlog #13: forgot password, end to end. Same shape as
-- vehicle_handover's confirmation code (SecureRandom-generated, hashed at
-- rest, expiring, single-use) rather than a new pattern.

CREATE TABLE password_reset_token (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_password_reset_token_user ON password_reset_token (user_id);
