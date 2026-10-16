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
-- COLLISION POLICY (deterministic, audited, transactional - any abort rolls back every statement of this file):
--   * Any object key (storage_key, evidence_key or thumbnail_key) referenced by more than one row  -> ABORT.
--   * A (job card, stage, sequence) group with more than one row is resolved ONLY when exactly one row is the
--     canonical evidence row (COMPLETED with an evidence_key) and every other row is FAILED with no evidence or
--     thumbnail key (a dead upload the processing worker never picks up again). The canonical row keeps the
--     sequence; each other row keeps its file name, status and storage references untouched, gets a NULL
--     media_sequence (the unique index is partial, so NULL rows are outside it) and is recorded in
--     job_card_media_sequence_conflict. Nothing is renamed, renumbered or deleted.
--   * Every other multi-row group (several canonical rows, no canonical row, a non-FAILED loser such as a pending
--     or uploaded row that could still be rendered over the canonical object)                         -> ABORT
--     with the group, ids and reason in the message. Resolve manually: docs/MEDIA_KEY_COLLISION_CHECK.md.
--   * The counter is seeded from the highest derived sequence of ALL rows (excluded rows included).

ALTER TABLE job_card_media
    ADD COLUMN media_sequence INT,
    ADD COLUMN staging_source_key VARCHAR(500),
    ADD COLUMN client_upload_id VARCHAR(100);

-- file names look like {jobCardNumber}_{STAGE}_{seq}.{ext}; rows whose name does not match stay NULL
UPDATE job_card_media
   SET media_sequence = CAST(substring(file_name FROM '_([0-9]+)\.[A-Za-z0-9]+$') AS INT)
 WHERE file_name ~ '_[0-9]+\.[A-Za-z0-9]+$';

-- audit trail for rows excluded from the sequence index by the policy above
CREATE TABLE job_card_media_sequence_conflict (
    id                 BIGSERIAL PRIMARY KEY,
    media_id           BIGINT       NOT NULL,
    job_card_id        BIGINT       NOT NULL,
    media_stage        VARCHAR(30)  NOT NULL,
    derived_sequence   INT          NOT NULL,
    canonical_media_id BIGINT       NOT NULL,
    upload_status      VARCHAR(20),
    file_name          VARCHAR(255),
    storage_key        VARCHAR(500),
    reason             VARCHAR(60)  NOT NULL,
    migration_version  VARCHAR(10)  NOT NULL DEFAULT '80',
    recorded_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_media_sequence_conflict_media UNIQUE (media_id)
);

DO $$
DECLARE
    key_collisions INT;
    unresolved INT;
    detail TEXT;
BEGIN
    -- 1. one object referenced by more than one row, through any key column
    SELECT count(*) INTO key_collisions FROM (
        SELECT key_value
          FROM (SELECT id, storage_key AS key_value FROM job_card_media
                UNION ALL SELECT id, evidence_key FROM job_card_media
                UNION ALL SELECT id, thumbnail_key FROM job_card_media) k
         WHERE key_value IS NOT NULL
         GROUP BY key_value
        HAVING count(DISTINCT id) > 1) c;

    -- 2. multi-row sequence groups that the policy cannot resolve
    CREATE TEMPORARY TABLE v80_groups ON COMMIT DROP AS
    SELECT job_card_id, media_stage, media_sequence,
           count(*) AS rows_in_group,
           count(*) FILTER (WHERE upload_status = 'COMPLETED' AND evidence_key IS NOT NULL) AS canonical_rows,
           count(*) FILTER (WHERE NOT (upload_status = 'COMPLETED' AND evidence_key IS NOT NULL)
                                  AND NOT (upload_status = 'FAILED' AND evidence_key IS NULL AND thumbnail_key IS NULL)) AS blocking_rows,
           array_agg(id ORDER BY id) AS media_ids
      FROM job_card_media
     WHERE media_sequence IS NOT NULL
     GROUP BY job_card_id, media_stage, media_sequence
    HAVING count(*) > 1;

    SELECT count(*), string_agg(
               format('job_card_id=%s stage=%s sequence=%s media_ids=%s canonical_rows=%s blocking_rows=%s',
                      job_card_id, media_stage, media_sequence, media_ids, canonical_rows, blocking_rows), '; ')
      INTO unresolved, detail
      FROM v80_groups
     WHERE canonical_rows <> 1 OR blocking_rows > 0;

    IF key_collisions > 0 OR unresolved > 0 THEN
        RAISE EXCEPTION
            'V80 aborted: % media sequence collision group(s) cannot be resolved automatically and % object-key collision group(s) exist. '
            'Original evidence may have been overwritten; resolve manually (docs/MEDIA_KEY_COLLISION_CHECK.md) '
            'before retrying. No data was changed. Unresolved groups: %',
            unresolved, key_collisions, COALESCE(detail, 'none');
    END IF;

    -- 3. resolvable groups: record every non-canonical row, then take it out of the sequence index
    INSERT INTO job_card_media_sequence_conflict
        (media_id, job_card_id, media_stage, derived_sequence, canonical_media_id,
         upload_status, file_name, storage_key, reason)
    SELECT m.id, m.job_card_id, m.media_stage, m.media_sequence, c.id,
           m.upload_status, m.file_name, m.storage_key, 'SUPERSEDED_BY_COMPLETED_EVIDENCE_ROW'
      FROM job_card_media m
      JOIN v80_groups g USING (job_card_id, media_stage, media_sequence)
      JOIN job_card_media c ON c.job_card_id = m.job_card_id AND c.media_stage = m.media_stage
                           AND c.media_sequence = m.media_sequence
                           AND c.upload_status = 'COMPLETED' AND c.evidence_key IS NOT NULL
     WHERE NOT (m.upload_status = 'COMPLETED' AND m.evidence_key IS NOT NULL);

    CREATE TEMPORARY TABLE v80_seed ON COMMIT DROP AS
    SELECT job_card_id, media_stage, COALESCE(MAX(media_sequence), 0) AS last_seq
      FROM job_card_media GROUP BY job_card_id, media_stage;

    UPDATE job_card_media SET media_sequence = NULL
     WHERE id IN (SELECT media_id FROM job_card_media_sequence_conflict);
END $$;

CREATE TABLE job_card_media_counter (
    job_card_id BIGINT NOT NULL,
    media_stage VARCHAR(30) NOT NULL,
    last_seq INT NOT NULL,
    CONSTRAINT pk_job_card_media_counter PRIMARY KEY (job_card_id, media_stage)
);

-- seed strictly above every existing sequence (computed BEFORE conflict rows were nulled) so a new upload can
-- never reuse a historical number
INSERT INTO job_card_media_counter (job_card_id, media_stage, last_seq)
SELECT job_card_id, media_stage, last_seq FROM v80_seed;

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

-- derived assets: one object can belong to exactly one row
CREATE UNIQUE INDEX uk_job_card_media_evidence_key
    ON job_card_media (evidence_key)
    WHERE evidence_key IS NOT NULL;

CREATE UNIQUE INDEX uk_job_card_media_thumbnail_key
    ON job_card_media (thumbnail_key)
    WHERE thumbnail_key IS NOT NULL;
