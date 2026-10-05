-- GarageST - OTP verification
-- Additive only. One row per issued OTP. The OTP itself is never stored,
-- only an HMAC (otp_hash). A challenge is bound to (destination, purpose).
--
--   status: PENDING -> VERIFIED | EXPIRED | LOCKED | SUPERSEDED

CREATE TABLE otp_challenge (
    id BIGSERIAL PRIMARY KEY,
    destination VARCHAR(150) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    purpose VARCHAR(40) NOT NULL,
    user_id BIGINT NULL,
    otp_hash VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_reference VARCHAR(100) NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NULL
);

-- At most one live challenge per destination+purpose: concurrent sends collide here
-- instead of producing two valid OTPs.
CREATE UNIQUE INDEX uk_otp_challenge_pending
    ON otp_challenge (destination, purpose) WHERE status = 'PENDING';

CREATE INDEX idx_otp_challenge_destination_created
    ON otp_challenge (destination, created_at);
