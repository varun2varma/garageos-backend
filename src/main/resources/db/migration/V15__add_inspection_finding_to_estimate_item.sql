ALTER TABLE estimate_item
ADD COLUMN inspection_finding_id BIGINT;

ALTER TABLE estimate_item
ADD CONSTRAINT fk_estimate_item_inspection_finding
FOREIGN KEY (inspection_finding_id)
REFERENCES inspection_finding(id);

CREATE INDEX idx_estimate_item_inspection_finding
ON estimate_item(inspection_finding_id);