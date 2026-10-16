-- GarageST - Final report privacy remediation (additive, non-destructive).
--
-- Historical report rows are immutable audit records and are NEVER deleted or rewritten here. A row can
-- instead be WITHHELD: it stays in the table (bytes untouched) but is no longer served by any download
-- route, never used as the "latest" report, and never e-mailed or retried.
--
-- Owner reports issued before this release were built from ALL job card media, internal photos included,
-- so every existing OWNER row is withheld. A regenerated report (POST .../final-report/regenerate) is built
-- from CUSTOMER_VISIBLE media only and is served normally. Existing CUSTOMER rows were already built from
-- CUSTOMER_VISIBLE media at generation time and stay available; a later visibility downgrade of a photo
-- withholds the job card's reports at runtime (see MediaServiceImpl.updateVisibility).
--
-- E-mails already delivered before this release cannot be recalled.

ALTER TABLE job_card_final_report
    ADD COLUMN withheld BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN withheld_reason VARCHAR(80),
    ADD COLUMN withheld_at TIMESTAMP;

UPDATE job_card_final_report
   SET withheld = TRUE,
       withheld_reason = 'LEGACY_OWNER_REPORT_PRE_PRIVACY_POLICY',
       withheld_at = CURRENT_TIMESTAMP
 WHERE audience = 'OWNER';

CREATE INDEX idx_final_report_withheld ON job_card_final_report (job_card_id, audience, withheld);
