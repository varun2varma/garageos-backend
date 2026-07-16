CREATE TABLE job_card
(
    id BIGSERIAL PRIMARY KEY,

    job_card_number VARCHAR(30) NOT NULL UNIQUE,

    customer_id BIGINT NOT NULL,

    vehicle_id BIGINT NOT NULL,

    service_date DATE NOT NULL,

    status VARCHAR(30) NOT NULL,

    odometer_reading BIGINT NOT NULL,

    complaints TEXT,

    estimated_delivery_date DATE,

    remarks TEXT,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP,

    CONSTRAINT fk_job_card_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer(id),

    CONSTRAINT fk_job_card_vehicle
        FOREIGN KEY (vehicle_id)
        REFERENCES vehicle(id)
);

CREATE INDEX idx_job_card_number
ON job_card(job_card_number);

CREATE INDEX idx_job_card_customer
ON job_card(customer_id);

CREATE INDEX idx_job_card_vehicle
ON job_card(vehicle_id);

CREATE INDEX idx_job_card_status
ON job_card(status);

CREATE INDEX idx_job_card_service_date
ON job_card(service_date);