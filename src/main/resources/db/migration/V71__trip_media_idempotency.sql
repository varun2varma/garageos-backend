-- V71: idempotent driver evidence uploads.
-- A client generates one id per captured file and resends it on every retry; the server then returns the row it already
-- stored instead of creating a second metadata row and a second cloud object. Nullable and partial-unique, so every
-- existing row and every client that does not send an id behaves exactly as before.
ALTER TABLE navigation_trip_media
    ADD COLUMN client_upload_id VARCHAR(80);

CREATE UNIQUE INDEX uk_trip_media_client_upload
    ON navigation_trip_media (trip_id, client_upload_id)
    WHERE client_upload_id IS NOT NULL;
