CREATE TABLE vehicle_brand
(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    country VARCHAR(100),
    logo_url VARCHAR(300),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vehicle_brand_name
ON vehicle_brand(name);

CREATE TABLE vehicle_model
(
    id BIGSERIAL PRIMARY KEY,

    brand_id BIGINT NOT NULL,

    name VARCHAR(100) NOT NULL,

    body_type VARCHAR(30),

    seating_capacity INT,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_vehicle_model_brand
        FOREIGN KEY (brand_id)
        REFERENCES vehicle_brand(id)
);

CREATE INDEX idx_vehicle_model_brand
ON vehicle_model(brand_id);

CREATE TABLE vehicle_variant
(
    id BIGSERIAL PRIMARY KEY,

    model_id BIGINT NOT NULL,

    variant_name VARCHAR(100) NOT NULL,

    fuel_type VARCHAR(30) NOT NULL,

    transmission_type VARCHAR(30) NOT NULL,

    engine_cc INT,

    horsepower DOUBLE PRECISION,

    torque_nm DOUBLE PRECISION,

    launch_year INT,

    discontinued_year INT,

    service_interval_km INT,

    service_interval_months INT,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_vehicle_variant_model
        FOREIGN KEY (model_id)
        REFERENCES vehicle_model(id)
);

CREATE INDEX idx_vehicle_variant_model
ON vehicle_variant(model_id);

CREATE INDEX idx_vehicle_variant_lookup
ON vehicle_variant(model_id, fuel_type, transmission_type);