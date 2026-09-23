-- GarageOS - Vehicle Module
-- Mission backlog #1: RC (registration certificate) verification state.
-- NOT NULL with a NOT_SUBMITTED default so every existing vehicle stays
-- valid without a backfill step.

ALTER TABLE vehicle
    ADD COLUMN rc_verification_status VARCHAR(20) NOT NULL DEFAULT 'NOT_SUBMITTED',
    ADD COLUMN rc_document_reference VARCHAR(500) NULL,
    ADD COLUMN rc_verified_by BIGINT NULL,
    ADD COLUMN rc_verified_at TIMESTAMP NULL;
