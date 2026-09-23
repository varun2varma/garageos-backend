-- GarageOS - Repair Task Module
-- Mechanic work priority (Mission backlog #20). Belongs to RepairTask,
-- never JobCard's own global status. NOT NULL with a NORMAL default so
-- every existing row gets a sane value without a backfill step.

ALTER TABLE repair_task
    ADD COLUMN priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL';
