ALTER TABLE repair_task
ADD COLUMN job_assignment_id BIGINT;

ALTER TABLE repair_task
ADD CONSTRAINT fk_repair_task_job_assignment
    FOREIGN KEY (job_assignment_id)
    REFERENCES job_assignments(id);

CREATE INDEX idx_repair_task_job_assignment
    ON repair_task(job_assignment_id);
