-- Durable upload status for job_card_media, so an HTTP 200/201 from
-- POST /job-cards/{id}/media means "safely accepted/persisted, Drive upload
-- pending or complete" rather than "Drive upload definitely succeeded".
--
-- drive_file_id/drive_web_view_link were NOT NULL because a row was only
-- ever inserted after the Drive upload had already succeeded. That is
-- exactly the bug: nothing was ever persisted before/during a Drive
-- attempt, so a transient Drive failure or a dropped client connection left
-- no durable record at all. Rows are now inserted before the Drive attempt,
-- so both columns must become nullable until the attempt completes.

ALTER TABLE job_card_media
    ALTER COLUMN drive_file_id DROP NOT NULL;

ALTER TABLE job_card_media
    ALTER COLUMN drive_web_view_link DROP NOT NULL;

ALTER TABLE job_card_media
    ADD COLUMN upload_status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_retry_at TIMESTAMP NULL,
    ADD COLUMN last_error VARCHAR(500) NULL,
    ADD COLUMN local_storage_path VARCHAR(500) NULL;

-- Existing rows all correspond to already-completed Drive uploads (the old
-- code path never inserted a row otherwise), so the DEFAULT 'COMPLETED'
-- above already backfills them correctly with no separate UPDATE needed.

CREATE INDEX idx_job_card_media_upload_status
    ON job_card_media (upload_status, next_retry_at);
