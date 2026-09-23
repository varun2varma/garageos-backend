-- GarageOS - Garage Module
-- Adds optional latitude/longitude to garage, so a garage's location can
-- be used as the route origin for pickup/delivery ETA calculations (the
-- manager pre-acceptance map). Nullable and non-breaking: existing garages
-- keep working with address-only display until an owner/manager sets a
-- real map location for their garage.

ALTER TABLE garage
    ADD COLUMN latitude NUMERIC(10, 7) NULL,
    ADD COLUMN longitude NUMERIC(10, 7) NULL;
