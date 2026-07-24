CREATE TABLE inspection_finding
(
    id BIGSERIAL PRIMARY KEY,

    job_card_id BIGINT NOT NULL,

    inspection_master_item_id BIGINT NOT NULL,

    status VARCHAR(30) NOT NULL,

    remarks TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_finding_job_card
        FOREIGN KEY (job_card_id)
        REFERENCES job_card(id),

    CONSTRAINT fk_finding_master_item
        FOREIGN KEY (inspection_master_item_id)
        REFERENCES inspection_master_item(id)
);

CREATE INDEX idx_finding_job_card
ON inspection_finding(job_card_id);

CREATE INDEX idx_finding_master_item
ON inspection_finding(inspection_master_item_id);