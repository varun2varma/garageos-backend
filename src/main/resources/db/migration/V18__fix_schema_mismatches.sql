-- ===========================================================
-- V18__fix_schema_mismatches.sql
-- Fix remaining Hibernate schema validation mismatches
-- ===========================================================

---------------------------------------------------------------
-- 1. inspection_master
-- Rename transmission -> transmission_type
---------------------------------------------------------------

ALTER TABLE inspection_master
RENAME COLUMN transmission TO transmission_type;

---------------------------------------------------------------
-- 2. inspection_master_item
-- Add missing service-related columns
---------------------------------------------------------------

ALTER TABLE inspection_master_item
ADD COLUMN service_name VARCHAR(255);

ALTER TABLE inspection_master_item
ADD COLUMN service_description TEXT;

ALTER TABLE inspection_master_item
ADD COLUMN labour_cost NUMERIC(10,2) NOT NULL DEFAULT 0;

ALTER TABLE inspection_master_item
ADD COLUMN part_cost NUMERIC(10,2) NOT NULL DEFAULT 0;

---------------------------------------------------------------
-- Populate existing records
---------------------------------------------------------------

UPDATE inspection_master_item
SET
    service_name = check_item,
    service_description = description
WHERE service_name IS NULL;

---------------------------------------------------------------
-- Make service_name mandatory
---------------------------------------------------------------

ALTER TABLE inspection_master_item
ALTER COLUMN service_name SET NOT NULL;

---------------------------------------------------------------
-- 3. invoice
-- Add generated_at column
---------------------------------------------------------------

ALTER TABLE invoice
ADD COLUMN generated_at TIMESTAMP;

UPDATE invoice
SET generated_at = created_at
WHERE generated_at IS NULL;

ALTER TABLE invoice
ALTER COLUMN generated_at SET NOT NULL;