-- GarageOS - Media Module
-- Audit/evidence metadata for job_card_media: WHO captured it, WHEN, WHERE,
-- and WHO/WHEN it was uploaded, plus a reference to a derived "evidence"
-- image (original + a tasteful metadata overlay, generated server-side,
-- never overwriting the original). Purely additive; every existing row
-- (including all legacy Google Drive rows) gets NULL for every new column,
-- which is legitimate — no metadata was ever captured for those, and none
-- is fabricated retroactively.
--
-- uploaded_by_user_id is NOT duplicated here — the existing uploaded_by
-- column (from the original job_card_media table) already serves that role
-- and is already backend-authoritative (set from the authenticated
-- principal, never client-supplied). Only uploaded_at and an uploader name
-- snapshot are new.

ALTER TABLE job_card_media
    ADD COLUMN captured_at TIMESTAMP,
    ADD COLUMN captured_by_user_id BIGINT,
    ADD COLUMN captured_by_name_snapshot VARCHAR(200),
    ADD COLUMN latitude DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION,
    ADD COLUMN location_accuracy_meters DOUBLE PRECISION,
    ADD COLUMN location_name VARCHAR(255),
    ADD COLUMN uploaded_at TIMESTAMP,
    ADD COLUMN uploaded_by_name_snapshot VARCHAR(200),
    ADD COLUMN evidence_key VARCHAR(500);

CREATE INDEX idx_job_card_media_captured_by ON job_card_media (captured_by_user_id);
