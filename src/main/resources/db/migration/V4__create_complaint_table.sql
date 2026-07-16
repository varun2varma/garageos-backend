CREATE TABLE complaint
(
    id BIGSERIAL PRIMARY KEY,

    job_card_id BIGINT NOT NULL,

    complaint TEXT NOT NULL,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP,

    CONSTRAINT fk_complaint_job_card
        FOREIGN KEY (job_card_id)
        REFERENCES job_card(id)
);

CREATE INDEX idx_complaint_job_card
ON complaint(job_card_id);

CREATE INDEX idx_complaint_status
ON complaint(status);