CREATE TABLE inspection
(
    id BIGSERIAL PRIMARY KEY,

    complaint_id BIGINT NOT NULL,

    inspection_notes TEXT,

    recommended_work TEXT,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP,

    CONSTRAINT fk_inspection_complaint
        FOREIGN KEY (complaint_id)
        REFERENCES complaint(id)
);

CREATE INDEX idx_inspection_complaint
ON inspection(complaint_id);

CREATE INDEX idx_inspection_status
ON inspection(status);