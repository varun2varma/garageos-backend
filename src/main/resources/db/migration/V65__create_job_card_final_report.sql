-- GarageST - Final Job Card completion report (OWNER / CUSTOMER audiences)
-- Additive only. Issued documents are immutable: a regeneration inserts a new
-- version row, it never updates content of an existing one.

CREATE TABLE job_card_final_report (
    id BIGSERIAL PRIMARY KEY,
    job_card_id BIGINT NOT NULL,
    garage_id BIGINT NOT NULL,
    audience VARCHAR(20) NOT NULL,
    version INT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    file_size BIGINT NOT NULL,
    content BYTEA NOT NULL,
    generated_by BIGINT NULL,
    generated_at TIMESTAMP NOT NULL,
    email_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    email_attempts INT NOT NULL DEFAULT 0,
    email_recipient VARCHAR(150) NULL,
    email_sent_at TIMESTAMP NULL,
    email_next_attempt_at TIMESTAMP NULL,
    email_last_error VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_final_report_job_card FOREIGN KEY (job_card_id) REFERENCES job_card (id),
    CONSTRAINT uk_final_report_version UNIQUE (job_card_id, audience, version)
);

CREATE INDEX idx_final_report_job_card ON job_card_final_report (job_card_id, audience);
CREATE INDEX idx_final_report_email_retry
    ON job_card_final_report (email_next_attempt_at) WHERE email_status IN ('PENDING', 'FAILED');
