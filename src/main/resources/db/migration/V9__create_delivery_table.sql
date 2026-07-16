CREATE TABLE delivery
(
    id BIGSERIAL PRIMARY KEY,

    job_card_id BIGINT NOT NULL UNIQUE,

    invoice_id BIGINT NOT NULL UNIQUE,

    delivery_date_time TIMESTAMP,

    delivered_by VARCHAR(100),

    received_by VARCHAR(100),

    status VARCHAR(30) NOT NULL,

    remarks TEXT,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP,

    CONSTRAINT fk_delivery_job_card
        FOREIGN KEY (job_card_id)
        REFERENCES job_card(id),

    CONSTRAINT fk_delivery_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES invoice(id)
);

CREATE INDEX idx_delivery_job_card
ON delivery(job_card_id);

CREATE INDEX idx_delivery_invoice
ON delivery(invoice_id);

CREATE INDEX idx_delivery_status
ON delivery(status);