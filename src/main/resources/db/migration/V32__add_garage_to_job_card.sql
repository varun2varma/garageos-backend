ALTER TABLE job_card
ADD COLUMN garage_id BIGINT;

ALTER TABLE job_card
ADD CONSTRAINT fk_job_card_garage
    FOREIGN KEY (garage_id)
    REFERENCES garage(id);