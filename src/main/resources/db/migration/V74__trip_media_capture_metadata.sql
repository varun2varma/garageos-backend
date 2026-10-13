-- V74: audit metadata for driver pickup/delivery evidence. All columns nullable: existing rows keep working and render
-- "not recorded" for anything that was never captured (nothing is back-filled or invented).
--   captured_by_name   historical identity snapshot of the authenticated uploader (survives later renames)
--   captured_at_source DEVICE = the camera/device capture time was supplied and validated; SERVER = fell back to receipt time
--   accuracy_meters    GPS accuracy of the fix that produced latitude/longitude
--   location_address   resolved address / place name, only when the device could provide one
-- created_at (already present) remains the server-recorded acceptance time.
ALTER TABLE navigation_trip_media
    ADD COLUMN captured_by_name   VARCHAR(200),
    ADD COLUMN captured_at_source VARCHAR(10),
    ADD COLUMN accuracy_meters    DOUBLE PRECISION,
    ADD COLUMN location_address   VARCHAR(500);
