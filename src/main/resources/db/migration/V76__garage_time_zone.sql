-- V76: optional IANA timezone of a garage (e.g. Asia/Kolkata) used to display that garage event times. Nullable:
-- when unset the documented application fallback (Asia/Kolkata) applies. Stored timestamps are not changed.
ALTER TABLE garage
    ADD COLUMN time_zone VARCHAR(64);
