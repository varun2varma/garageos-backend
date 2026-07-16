CREATE TABLE invoice
(
    id BIGSERIAL PRIMARY KEY,

    invoice_number VARCHAR(30) NOT NULL UNIQUE,

    estimate_id BIGINT NOT NULL UNIQUE,

    invoice_status VARCHAR(30) NOT NULL,

    payment_status VARCHAR(30) NOT NULL,

    subtotal NUMERIC(12,2) NOT NULL,

    discount NUMERIC(12,2) NOT NULL,

    gst NUMERIC(12,2) NOT NULL,

    grand_total NUMERIC(12,2) NOT NULL,

    remarks TEXT,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP,

    CONSTRAINT fk_invoice_estimate
        FOREIGN KEY (estimate_id)
        REFERENCES estimate(id)
);

CREATE INDEX idx_invoice_number
ON invoice(invoice_number);

CREATE INDEX idx_invoice_estimate
ON invoice(estimate_id);

CREATE INDEX idx_invoice_payment_status
ON invoice(payment_status);

CREATE INDEX idx_invoice_status
ON invoice(invoice_status);