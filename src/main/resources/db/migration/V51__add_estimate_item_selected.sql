-- GarageOS - Estimate Item Module
-- Mission: customer can select/deselect individual estimate line items;
-- a deselected item must not continue into repair/invoice. NOT NULL with
-- a TRUE default so every existing item stays selected (no behavior
-- change for estimates already approved/in progress).

ALTER TABLE estimate_item
    ADD COLUMN selected BOOLEAN NOT NULL DEFAULT TRUE;
