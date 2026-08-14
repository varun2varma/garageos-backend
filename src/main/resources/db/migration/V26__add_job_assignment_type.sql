ALTER TABLE job_assignments
ADD COLUMN assignment_type VARCHAR(30) NOT NULL
DEFAULT 'TECHNICIAN';

CREATE INDEX idx_job_assignment_type
ON job_assignments(assignment_type);