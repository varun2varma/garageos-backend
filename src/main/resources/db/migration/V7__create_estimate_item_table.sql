CREATE TABLE estimate_item
(
    id BIGSERIAL PRIMARY KEY,

    estimate_id BIGINT NOT NULL,

    complaint_id BIGINT,

    item_type VARCHAR(20) NOT NULL,

    description VARCHAR(255) NOT NULL,

    quantity NUMERIC(10,2) NOT NULL,

    unit_price NUMERIC(12,2) NOT NULL,

    total_price NUMERIC(12,2) NOT NULL,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP,

    CONSTRAINT fk_estimate_item_estimate
        FOREIGN KEY (estimate_id)
        REFERENCES estimate(id),

    CONSTRAINT fk_estimate_item_complaint
        FOREIGN KEY (complaint_id)
        REFERENCES complaint(id)
);

CREATE INDEX idx_estimate_item_estimate
ON estimate_item(estimate_id);

CREATE INDEX idx_estimate_item_complaint
ON estimate_item(complaint_id);

CREATE INDEX idx_estimate_item_type
ON estimate_item(item_type);