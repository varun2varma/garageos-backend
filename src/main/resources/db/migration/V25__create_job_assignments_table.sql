CREATE TABLE job_assignments
(

    id BIGSERIAL PRIMARY KEY,

    garage_id BIGINT NOT NULL,

    job_card_id BIGINT NOT NULL,

    estimate_item_id BIGINT,

    user_id BIGINT NOT NULL,

    assigned_by BIGINT,

    status VARCHAR(50) NOT NULL,

    assigned_at TIMESTAMP NOT NULL,

    accepted_at TIMESTAMP,

    started_at TIMESTAMP,

    completed_at TIMESTAMP,

    estimated_hours DOUBLE PRECISION,

    actual_hours DOUBLE PRECISION,

    remarks VARCHAR(1000),

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP,

    CONSTRAINT fk_job_assignment_garage
        FOREIGN KEY (garage_id)
            REFERENCES garage(id),

    CONSTRAINT fk_job_assignment_job_card
        FOREIGN KEY (job_card_id)
            REFERENCES job_card(id),

    CONSTRAINT fk_job_assignment_estimate_item
        FOREIGN KEY (estimate_item_id)
            REFERENCES estimate_item(id),

    CONSTRAINT fk_job_assignment_user
        FOREIGN KEY (user_id)
            REFERENCES users(id),

    CONSTRAINT fk_job_assignment_assigned_by
        FOREIGN KEY (assigned_by)
            REFERENCES users(id)

);

CREATE INDEX idx_job_assignment_job_card
    ON job_assignments(job_card_id);

CREATE INDEX idx_job_assignment_user
    ON job_assignments(user_id);

CREATE INDEX idx_job_assignment_status
    ON job_assignments(status);

CREATE INDEX idx_job_assignment_estimate_item
    ON job_assignments(estimate_item_id);

CREATE INDEX idx_job_assignment_garage
    ON job_assignments(garage_id);