--/*
--==========================================================
-- GarageOS - Vehicle Handover (secure pickup/delivery confirmation)
-- V38
--==========================================================
--*/

CREATE TABLE vehicle_handover (

    id BIGSERIAL PRIMARY KEY,

    trip_id BIGINT NOT NULL,

    direction VARCHAR(30) NOT NULL,

    code_hash VARCHAR(255) NOT NULL,

    status VARCHAR(30) NOT NULL,

    expires_at TIMESTAMP NOT NULL,

    verified_at TIMESTAMP,

    verified_by_driver_id BIGINT,

    failed_attempts INT NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_vehicle_handover_trip
        FOREIGN KEY (trip_id)
        REFERENCES navigation_trip(id)

);

CREATE INDEX idx_vehicle_handover_trip
    ON vehicle_handover(trip_id);

CREATE INDEX idx_vehicle_handover_status
    ON vehicle_handover(status);
