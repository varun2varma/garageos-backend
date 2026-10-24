-- Shared, multi-instance rate-limit counters (see com.garageos.core.ratelimit.RateLimitService).
-- One row per (policy + hashed subject): a fixed window that restarts once expires_at has passed.
-- bucket_key holds a SHA-256 digest of the subject, never a raw phone number, e-mail or token.
-- Additive and self-pruning: a scheduled job deletes rows that expired more than an hour ago.

CREATE TABLE rate_limit_bucket (
    bucket_key   VARCHAR(160) PRIMARY KEY,
    window_start TIMESTAMP    NOT NULL,
    hit_count    INTEGER      NOT NULL,
    expires_at   TIMESTAMP    NOT NULL
);

CREATE INDEX idx_rate_limit_bucket_expires ON rate_limit_bucket (expires_at);
