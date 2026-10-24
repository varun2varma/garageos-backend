-- Garage-specific colour palette (primary / secondary) next to the existing logo columns (V63).
-- Both are optional: NULL means "use the GarageST default theme". Stored as upper-case #RRGGBB, enforced here as well
-- as in the service. Additive and nullable, so every existing garage is unaffected.

ALTER TABLE garage
    ADD COLUMN brand_primary_color   VARCHAR(7),
    ADD COLUMN brand_secondary_color VARCHAR(7),
    ADD COLUMN branding_updated_at   TIMESTAMP,
    ADD COLUMN branding_updated_by   BIGINT,
    ADD CONSTRAINT ck_garage_brand_primary   CHECK (brand_primary_color   IS NULL OR brand_primary_color   ~ '^#[0-9A-F]{6}$'),
    ADD CONSTRAINT ck_garage_brand_secondary CHECK (brand_secondary_color IS NULL OR brand_secondary_color ~ '^#[0-9A-F]{6}$');
