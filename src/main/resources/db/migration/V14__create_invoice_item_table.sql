CREATE TABLE invoice_item
(
    id BIGSERIAL PRIMARY KEY,

    invoice_id BIGINT NOT NULL,

    complaint_id BIGINT,

    item_type VARCHAR(30) NOT NULL,

    description VARCHAR(255) NOT NULL,

    quantity INTEGER NOT NULL,

    unit_price NUMERIC(12,2) NOT NULL,

    total_price NUMERIC(12,2) NOT NULL,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_invoice_item_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES invoice(id),

    CONSTRAINT fk_invoice_item_complaint
        FOREIGN KEY (complaint_id)
        REFERENCES complaint(id)
);

CREATE INDEX idx_invoice_item_invoice
ON invoice_item(invoice_id);

CREATE INDEX idx_invoice_item_complaint
ON invoice_item(complaint_id);

CREATE INDEX idx_invoice_item_type
ON invoice_item(item_type);