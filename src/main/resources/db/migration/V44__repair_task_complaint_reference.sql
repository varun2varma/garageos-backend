ALTER TABLE repair_task
    ADD COLUMN complaint_id BIGINT;

ALTER TABLE repair_task
    ADD CONSTRAINT fk_repair_task_complaint
        FOREIGN KEY (complaint_id)
        REFERENCES complaint(id);

ALTER TABLE repair_task
    ALTER COLUMN estimate_item_id DROP NOT NULL;

CREATE INDEX idx_repair_task_job_card_complaint
    ON repair_task(job_card_id, complaint_id);