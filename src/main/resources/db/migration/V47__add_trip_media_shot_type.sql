-- GarageOS - Navigation Module
-- Adds an optional shot-type tag to navigation_trip_media (FRONT/REAR/
-- LEFT/RIGHT/INTERIOR/ODOMETER/OTHER) so before-pickup/delivery evidence
-- requirements can be enforced per-angle (Mission Part I) instead of just
-- "at least one photo exists". Nullable: rows captured before this column
-- existed remain valid, just without this metadata.

ALTER TABLE navigation_trip_media
    ADD COLUMN shot_type VARCHAR(20) NULL;
