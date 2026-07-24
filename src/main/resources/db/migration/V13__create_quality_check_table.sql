CREATE TABLE quality_check
(
    id BIGSERIAL PRIMARY KEY,

    job_card_id BIGINT NOT NULL UNIQUE,

    status VARCHAR(30) NOT NULL,

    inspected_by VARCHAR(100),

    inspected_at TIMESTAMP,

    remarks TEXT,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_quality_check_job_card
        FOREIGN KEY (job_card_id)
        REFERENCES job_card(id)
);

CREATE INDEX idx_quality_check_job_card
ON quality_check(job_card_id);

CREATE INDEX idx_quality_check_status
ON quality_check(status);