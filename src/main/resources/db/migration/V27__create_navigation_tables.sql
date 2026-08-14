CREATE TABLE IF NOT EXISTS driver_current_location (
    id BIGSERIAL PRIMARY KEY,

    driver_id BIGINT NOT NULL,

    trip_id BIGINT NOT NULL,

    latitude DOUBLE PRECISION NOT NULL,

    longitude DOUBLE PRECISION NOT NULL,

    speed DOUBLE PRECISION,

    heading DOUBLE PRECISION,

    accuracy DOUBLE PRECISION,

    last_updated TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_current_location_driver
ON driver_current_location (driver_id);

CREATE INDEX IF NOT EXISTS idx_current_location_trip
ON driver_current_location (trip_id);


CREATE TABLE IF NOT EXISTS driver_location_history (
    id BIGSERIAL PRIMARY KEY,

    driver_id BIGINT NOT NULL,

    trip_id BIGINT NOT NULL,

    latitude DOUBLE PRECISION NOT NULL,

    longitude DOUBLE PRECISION NOT NULL,

    speed DOUBLE PRECISION,

    heading DOUBLE PRECISION,

    accuracy DOUBLE PRECISION,

    location_time TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_location_history_driver
ON driver_location_history (driver_id);

CREATE INDEX IF NOT EXISTS idx_location_history_trip
ON driver_location_history (trip_id);

CREATE INDEX IF NOT EXISTS idx_location_history_time
ON driver_location_history (location_time);