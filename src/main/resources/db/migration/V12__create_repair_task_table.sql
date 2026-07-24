CREATE TABLE repair_task
(
    id BIGSERIAL PRIMARY KEY,

    job_card_id BIGINT NOT NULL,

    estimate_item_id BIGINT NOT NULL,

    status VARCHAR(30) NOT NULL,

    technician_name VARCHAR(100),

    assigned_at TIMESTAMP,

    started_at TIMESTAMP,

    completed_at TIMESTAMP,

    remarks TEXT,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_repair_task_job_card
        FOREIGN KEY (job_card_id)
        REFERENCES job_card(id),

    CONSTRAINT fk_repair_task_estimate_item
        FOREIGN KEY (estimate_item_id)
        REFERENCES estimate_item(id)
);

CREATE INDEX idx_repair_task_job_card
ON repair_task(job_card_id);

CREATE INDEX idx_repair_task_status
ON repair_task(status);

CREATE INDEX idx_repair_task_estimate_item
ON repair_task(estimate_item_id);