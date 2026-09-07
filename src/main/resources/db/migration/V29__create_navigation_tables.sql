--/*
--==========================================================
-- GarageOS - Navigation Module
-- V29
--==========================================================
--*/

CREATE TABLE navigation_request (

    id BIGSERIAL PRIMARY KEY,

    customer_id BIGINT NOT NULL,

    vehicle_id BIGINT NOT NULL,

    garage_id BIGINT NOT NULL,

    request_type VARCHAR(30) NOT NULL,

    pickup_address TEXT,

    delivery_address TEXT,

    scheduled_at TIMESTAMP NOT NULL,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

);


CREATE INDEX idx_navigation_request_customer
    ON navigation_request(customer_id);

CREATE INDEX idx_navigation_request_vehicle
    ON navigation_request(vehicle_id);

CREATE INDEX idx_navigation_request_garage
    ON navigation_request(garage_id);

CREATE INDEX idx_navigation_request_status
    ON navigation_request(status);

CREATE INDEX idx_navigation_request_scheduled
    ON navigation_request(scheduled_at);



CREATE TABLE navigation_trip (

    id BIGSERIAL PRIMARY KEY,

    navigation_request_id BIGINT NOT NULL,

    vehicle_id BIGINT NOT NULL,

    driver_id BIGINT,

    trip_type VARCHAR(30) NOT NULL,

    current_leg VARCHAR(40) NOT NULL,

    status VARCHAR(30) NOT NULL,

    source_address TEXT,

    destination_address TEXT,

    accepted_at TIMESTAMP,

    started_at TIMESTAMP,

    arrived_at TIMESTAMP,

    completed_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_navigation_trip_request

        FOREIGN KEY (navigation_request_id)

        REFERENCES navigation_request(id)

);


CREATE INDEX idx_navigation_trip_request
    ON navigation_trip(navigation_request_id);

CREATE INDEX idx_navigation_trip_driver
    ON navigation_trip(driver_id);

CREATE INDEX idx_navigation_trip_vehicle
    ON navigation_trip(vehicle_id);

CREATE INDEX idx_navigation_trip_status
    ON navigation_trip(status);

CREATE INDEX idx_navigation_trip_leg
    ON navigation_trip(current_leg);



CREATE TABLE navigation_trip_media (

    id BIGSERIAL PRIMARY KEY,

    trip_id BIGINT NOT NULL,

    media_stage VARCHAR(30) NOT NULL,

    storage_key TEXT NOT NULL,

    file_name VARCHAR(255),

    content_type VARCHAR(100),

    file_size BIGINT,

    captured_by BIGINT NOT NULL,

    captured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    latitude DOUBLE PRECISION,

    longitude DOUBLE PRECISION,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_trip_media_trip

        FOREIGN KEY (trip_id)

        REFERENCES navigation_trip(id)

);


CREATE INDEX idx_trip_media_trip
    ON navigation_trip_media(trip_id);

CREATE INDEX idx_trip_media_stage
    ON navigation_trip_media(media_stage);