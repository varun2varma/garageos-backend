ALTER TABLE inspection_finding
ADD COLUMN complaint_id BIGINT;

ALTER TABLE inspection_finding
ADD CONSTRAINT fk_inspection_finding_complaint
FOREIGN KEY (complaint_id)
REFERENCES complaint(id);

CREATE INDEX idx_inspection_finding_complaint
ON inspection_finding(complaint_id);