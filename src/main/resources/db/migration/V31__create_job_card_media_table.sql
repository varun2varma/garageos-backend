CREATE TABLE job_card_media
(
    id BIGSERIAL PRIMARY KEY,

    job_card_id BIGINT NOT NULL,

    file_name VARCHAR(255) NOT NULL,

    drive_file_id VARCHAR(255) NOT NULL,

    drive_web_view_link TEXT,

    media_type VARCHAR(20) NOT NULL,

    media_stage VARCHAR(30) NOT NULL,

    content_type VARCHAR(100) NOT NULL,

    file_size BIGINT NOT NULL,

    uploaded_by BIGINT,

    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_job_card_media_job_card
        FOREIGN KEY (job_card_id)
        REFERENCES job_card(id),

    CONSTRAINT chk_job_card_media_type
        CHECK (media_type IN ('IMAGE', 'VIDEO')),

    CONSTRAINT chk_job_card_media_stage
        CHECK (
            media_stage IN (
                'BEFORE_SERVICE',
                'DURING_REPAIR',
                'AFTER_REPAIR'
            )
        )
);

CREATE INDEX idx_job_card_media_job_card
ON job_card_media(job_card_id);

CREATE INDEX idx_job_card_media_stage
ON job_card_media(job_card_id, media_stage);

CREATE INDEX idx_job_card_media_type
ON job_card_media(job_card_id, media_type);

CREATE INDEX idx_job_card_media_drive_file
ON job_card_media(drive_file_id);