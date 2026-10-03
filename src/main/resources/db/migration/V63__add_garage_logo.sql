-- Garage-specific branding: each garage (a separate business on the GarageST
-- platform) can have its own logo. The logo belongs to the GARAGE, never to a
-- user/owner/customer. All columns are nullable so every existing garage keeps
-- working unchanged and falls back to GarageST default branding until an owner
-- uploads one (at registration or later from the owner dashboard).
--
-- logo_storage_key is a key in the existing MediaStorageService (the same
-- storage abstraction navigation trip media already uses), never a public URL;
-- the bytes are only ever served through the authorized
-- GET /api/v1/garages/{id}/logo endpoint.

ALTER TABLE garage
    ADD COLUMN logo_storage_key   VARCHAR(500),
    ADD COLUMN logo_content_type  VARCHAR(100),
    ADD COLUMN logo_updated_at    TIMESTAMP;
