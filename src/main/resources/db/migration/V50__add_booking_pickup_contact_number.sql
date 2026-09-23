-- GarageOS - Booking Module
-- Mission backlog #2: an optional alternate pickup contact number (POC),
-- when different from the customer account's own mobile.

ALTER TABLE booking
    ADD COLUMN pickup_contact_number VARCHAR(20) NULL;
