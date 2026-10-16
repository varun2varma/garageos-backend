-- GarageST - media storage-key integrity (D04).
--
-- Problem: the media sequence was "read max + 1" with no lock, so two concurrent uploads for the same
-- job card and stage could receive the same sequence and therefore the same object-storage key. The second
-- upload then overwrote the first one's bytes while both rows pointed at one object set, and deleting one
-- row deleted the evidence the other still needed.
--
-- This migration is ADDITIVE and NEVER reassigns, renames or deletes anything:
--   1. media_sequence  - the sequence number as an explicit column (backfilled from the file name).
--   2. staging_source_key - the temporary "_source" object of an image awaiting evidence rendering, persisted
--      so a restart cannot orphan it (a sweeper deletes it once the row is COMPLETED).
--   3. job_card_media_counter - atomic per (job card, stage) counter; seeded above every existing sequence.
--   4. uniqueness: (job_card_id, media_stage, media_sequence) and storage_key.
--
-- PRECONDITION (fails the migration safely instead of altering evidence): if existing rows already collide,
-- the unique indexes cannot be created. A collision means original bytes may already have been overwritten,
-- so rows are never auto-reassigned. Resolve them manually first - see docs/MEDIA_KEY_COLLISION_CHECK.md for
-- the read-only detection query and the procedure.

ALTER TABLE job_card_media
    ADD COLUMN media_sequence INT,
    ADD COLUMN staging_source_key VARCHAR(500),
    ADD COLUMN client_upload_id VARCHAR(100);

-- file names look like {jobCardNumber}_{STAGE}_{seq}.{ext}; rows whose name does not match stay NULL
UPDATE job_card_media
   SET media_sequence = CAST(substring(file_name FROM '_([0-9]+)\.[A-Za-z0-9]+$') AS INT)
 WHERE file_name ~ '_[0-9]+\.[A-Za-z0-9]+$';

DO $$
DECLARE
    seq_collisions INT;
    key_collisions INT;
BEGIN
    SELECT count(*) INTO seq_collisions FROM (
        SELECT 1 FROM job_card_media
         WHERE media_sequence IS NOT NULL
         GROUP BY job_card_id, media_stage, media_sequence
        HAVING count(*) > 1) c;

    SELECT count(*) INTO key_collisions FROM (
        SELECT 1 FROM job_card_media
         WHERE storage_key IS NOT NULL
         GROUP BY storage_key
        HAVING count(*) > 1) c;

    IF seq_collisions > 0 OR key_collisions > 0 THEN
        RAISE EXCEPTION
            'V80 aborted: % media sequence collision group(s) and % storage_key collision group(s) exist. '
            'Original evidence may have been overwritten; resolve manually (docs/MEDIA_KEY_COLLISION_CHECK.md) '
            'before retrying. No data was changed.', seq_collisions, key_collisions;
    END IF;
END $$;

CREATE TABLE job_card_media_counter (
    job_card_id BIGINT NOT NULL,
    media_stage VARCHAR(30) NOT NULL,
    last_seq INT NOT NULL,
    CONSTRAINT pk_job_card_media_counter PRIMARY KEY (job_card_id, media_stage)
);

-- seed strictly above every existing sequence so a new upload can never reuse a historical number
INSERT INTO job_card_media_counter (job_card_id, media_stage, last_seq)
SELECT job_card_id, media_stage, COALESCE(MAX(media_sequence), 0)
  FROM job_card_media
 GROUP BY job_card_id, media_stage;

CREATE UNIQUE INDEX uk_job_card_media_sequence
    ON job_card_media (job_card_id, media_stage, media_sequence)
    WHERE media_sequence IS NOT NULL;

-- idempotent upload intents: a retry carrying the same client id maps to the same row
CREATE UNIQUE INDEX uk_job_card_media_client_upload
    ON job_card_media (job_card_id, client_upload_id)
    WHERE client_upload_id IS NOT NULL;

CREATE UNIQUE INDEX uk_job_card_media_storage_key
    ON job_card_media (storage_key)
    WHERE storage_key IS NOT NULL;
