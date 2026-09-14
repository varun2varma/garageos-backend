ALTER TABLE job_card_media
ADD COLUMN repair_task_id BIGINT;

ALTER TABLE job_card_media
ADD COLUMN visibility VARCHAR(30) NOT NULL DEFAULT 'INTERNAL';

ALTER TABLE job_card_media
ADD CONSTRAINT fk_job_card_media_repair_task
    FOREIGN KEY (repair_task_id)
    REFERENCES repair_task(id);

CREATE INDEX idx_job_card_media_repair_task
    ON job_card_media(repair_task_id);

CREATE INDEX idx_job_card_media_visibility
    ON job_card_media(job_card_id, visibility);