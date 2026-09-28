-- GarageOS - Identity Module
-- Google Play external account-deletion requirement: records a deletion
-- request submitted anonymously (email/mobile) from the public
-- /delete-account page. Same shape as password_reset_token (BIGSERIAL id,
-- FK to users, status/timestamps) - no cascade delete on the FK, since a
-- deletion request row should still exist for audit purposes even after
-- the referenced user's own row is later anonymized.

CREATE TABLE account_deletion_request (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    verified_at TIMESTAMP,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_account_deletion_request_user ON account_deletion_request (user_id);
CREATE INDEX idx_account_deletion_request_status ON account_deletion_request (status);
