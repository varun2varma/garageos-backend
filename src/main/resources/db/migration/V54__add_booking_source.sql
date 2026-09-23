-- GarageOS - Booking Module
-- Mission backlog #6: phone-call booking. NOT NULL with a CUSTOMER_APP
-- default so every existing booking (all customer-app-created) stays
-- correct with no backfill step.

ALTER TABLE booking
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER_APP',
    ADD COLUMN created_by_employee_id BIGINT NULL,
    ADD COLUMN notes VARCHAR(1000) NULL;
