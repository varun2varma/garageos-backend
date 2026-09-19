ALTER TABLE job_assignments
    ADD COLUMN repair_task_id BIGINT;

ALTER TABLE job_assignments
    ADD CONSTRAINT fk_job_assignments_repair_task
        FOREIGN KEY (repair_task_id)
        REFERENCES repair_task(id);

CREATE INDEX idx_job_assignments_repair_task_id
    ON job_assignments(repair_task_id);