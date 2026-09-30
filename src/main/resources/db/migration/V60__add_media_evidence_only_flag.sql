-- GarageOS - Media Module
-- Marks rows created under the final "evidence + thumbnail only" model: no
-- permanent original is ever kept in R2 for these rows. For an IMAGE the
-- client-uploaded object is a temporary staging "source" that the backend
-- replaces with the rendered evidence image (storage_key is repointed to
-- it); for a VIDEO the uploaded object already IS the burned-in evidence
-- MP4. Every pre-existing row keeps FALSE and continues to be served
-- exactly as before (original + optional evidence/thumbnail). Additive only.

ALTER TABLE job_card_media
    ADD COLUMN evidence_only BOOLEAN NOT NULL DEFAULT FALSE;
