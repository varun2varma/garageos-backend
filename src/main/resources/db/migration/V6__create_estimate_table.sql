CREATE TABLE estimate
(
    id BIGSERIAL PRIMARY KEY,

    estimate_number VARCHAR(30) NOT NULL UNIQUE,

    job_card_id BIGINT NOT NULL,

    status VARCHAR(30) NOT NULL,

    subtotal NUMERIC(12,2) NOT NULL,

    discount NUMERIC(12,2) NOT NULL,

    gst NUMERIC(12,2) NOT NULL,

    grand_total NUMERIC(12,2) NOT NULL,

    remarks TEXT,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP,

    CONSTRAINT fk_estimate_job_card
        FOREIGN KEY (job_card_id)
        REFERENCES job_card(id)
);

CREATE INDEX idx_estimate_number
ON estimate(estimate_number);

CREATE INDEX idx_estimate_job_card
ON estimate(job_card_id);

CREATE INDEX idx_estimate_status
ON estimate(status);