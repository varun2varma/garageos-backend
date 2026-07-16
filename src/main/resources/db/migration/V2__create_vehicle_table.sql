CREATE TABLE vehicle
(
    id BIGSERIAL PRIMARY KEY,

    registration_number VARCHAR(20) NOT NULL UNIQUE,

    brand VARCHAR(100) NOT NULL,

    model VARCHAR(100) NOT NULL,

    variant VARCHAR(100),

    fuel_type VARCHAR(50),

    transmission VARCHAR(50),

    manufacturing_year INTEGER,

    color VARCHAR(50),

    customer_id BIGINT NOT NULL,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP,

    CONSTRAINT fk_vehicle_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer(id)
);

CREATE INDEX idx_vehicle_registration
ON vehicle(registration_number);

CREATE INDEX idx_vehicle_customer
ON vehicle(customer_id);