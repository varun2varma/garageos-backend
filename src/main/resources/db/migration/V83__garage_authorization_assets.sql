-- GarageST - the garage authorized signature and stamp printed on the final invoice PDF (D14).
--
-- Private assets owned by the garage (never by a user and never publicly served): only the storage key, content
-- type and update time are kept here; the image bytes live in the private object store (or local disk fallback),
-- exactly like the garage logo. Nullable: a garage uploads them once, and the final invoice PDF is refused
-- (with an actionable message) until both exist - see InvoicePdfServiceImpl for the legacy-invoice policy.

ALTER TABLE garage
    ADD COLUMN signature_storage_key VARCHAR(500),
    ADD COLUMN signature_content_type VARCHAR(100),
    ADD COLUMN signature_updated_at TIMESTAMP,
    ADD COLUMN stamp_storage_key VARCHAR(500),
    ADD COLUMN stamp_content_type VARCHAR(100),
    ADD COLUMN stamp_updated_at TIMESTAMP;
