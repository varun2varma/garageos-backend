--/*
--==========================================================
-- GarageOS - Booking Module
-- V37
--==========================================================
--*/

CREATE TABLE booking (

    id BIGSERIAL PRIMARY KEY,

    customer_id BIGINT NOT NULL,

    vehicle_id BIGINT NOT NULL,

    garage_id BIGINT NOT NULL,

    service_description VARCHAR(1000) NOT NULL,

    concerns VARCHAR(2000),

    requested_at TIMESTAMP NOT NULL,

    pickup_requested BOOLEAN NOT NULL DEFAULT FALSE,

    pickup_address VARCHAR(500),

    status VARCHAR(30) NOT NULL,

    garage_remarks VARCHAR(1000),

    navigation_request_id BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

);

CREATE INDEX idx_booking_customer
    ON booking(customer_id);

CREATE INDEX idx_booking_garage
    ON booking(garage_id);

CREATE INDEX idx_booking_status
    ON booking(status);


-- Additive, nullable link from an existing JobCard back to the booking it
-- originated from, when applicable. Every historical JobCard remains valid
-- with this column NULL - a JobCard is not required to have a booking.
ALTER TABLE job_card
    ADD COLUMN booking_id BIGINT NULL;

CREATE INDEX idx_job_card_booking
    ON job_card(booking_id);
