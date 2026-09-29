-- GarageOS - Media Module
-- Introduces a provider-neutral storage model on top of job_card_media so a
-- second storage backend (Cloudflare R2) can be added without disturbing any
-- existing Google Drive-backed row. Purely additive:
--   - storage_provider defaults to GOOGLE_DRIVE, so every existing row keeps
--     its current (already-working) playback path unchanged.
--   - drive_file_id / drive_web_view_link are untouched — still the
--     authoritative reference for GOOGLE_DRIVE rows.
--   - storage_key is the R2 (or future provider) equivalent of drive_file_id;
--     null for GOOGLE_DRIVE rows.
--   - checksum/upload_session_id support upload-completion idempotency for
--     the new direct-upload flow (see MediaServiceImpl.completeUpload).
--   - thumbnail_key/duration_seconds are derived-asset metadata, populated
--     once async processing (MediaProcessingScheduler) completes.

ALTER TABLE job_card_media
    ADD COLUMN storage_provider VARCHAR(20) NOT NULL DEFAULT 'GOOGLE_DRIVE',
    ADD COLUMN storage_key VARCHAR(500),
    ADD COLUMN checksum VARCHAR(128),
    ADD COLUMN upload_session_id VARCHAR(100),
    ADD COLUMN thumbnail_key VARCHAR(500),
    ADD COLUMN duration_seconds INTEGER;

CREATE INDEX idx_job_card_media_storage_provider ON job_card_media (storage_provider);

-- Partial unique index: only enforced where an upload_session_id is actually
-- set, so existing (null) rows are unaffected and a completion request can
-- be safely deduplicated by session id (see completeUpload's idempotency
-- check) without a NOT NULL constraint forcing every legacy row to have one.
CREATE UNIQUE INDEX idx_job_card_media_upload_session
    ON job_card_media (upload_session_id)
    WHERE upload_session_id IS NOT NULL;
