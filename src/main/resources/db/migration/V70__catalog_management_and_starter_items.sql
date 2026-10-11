--==========================================================
-- GarageST - Catalog management support + starter catalog
-- V70 (forward-only; builds on V69)
--
-- 1. Category, classification provenance, audit and optimistic-lock
--    columns on catalog_item; source/verification on gst_tax_rule.
-- 2. A small starter catalog of generic workshop part and service NAMES.
--
-- The starter rows carry NO HSN/SAC code, NO tax rule and NO rate: they are
-- UNCLASSIFIED until an authorised reviewer classifies and verifies them
-- through the catalog API. No GST rule is created here. Garage prices and
-- stock are never seeded. The inserts are idempotent (ON CONFLICT DO NOTHING
-- on the unique (item_type, name_normalized) index), so re-running or an
-- item an administrator already created is never duplicated or overwritten.
--==========================================================

ALTER TABLE catalog_item
    ADD COLUMN category VARCHAR(60),
    ADD COLUMN classification_source VARCHAR(255),
    ADD COLUMN classification_verified_at TIMESTAMP,
    ADD COLUMN classification_verified_by_user_id BIGINT,
    ADD COLUMN updated_by_user_id BIGINT,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- A newly verified item must say where its classification came from.
-- NOT VALID: rows that predate this rule are not rewritten; every new or
-- changed row is checked.
ALTER TABLE catalog_item
    ADD CONSTRAINT ck_catalog_item_verified_source
        CHECK (classification_status <> 'VERIFIED'
               OR (classification_source IS NOT NULL AND classification_verified_at IS NOT NULL))
        NOT VALID;

CREATE INDEX idx_catalog_item_category ON catalog_item(item_type, category);

ALTER TABLE gst_tax_rule
    ADD COLUMN source_reference VARCHAR(255),
    ADD COLUMN verified_on DATE,
    ADD COLUMN created_by_user_id BIGINT;

-- ---------------------------------------------------------
-- Starter catalog: generic names only, UNCLASSIFIED
-- ---------------------------------------------------------
INSERT INTO catalog_item
    (item_type, name, name_normalized, description, default_unit, category, classification_status, active)
SELECT v.item_type, v.name, lower(v.name), v.description, v.default_unit, v.category, 'UNCLASSIFIED', TRUE
FROM (VALUES
    ('PART', 'Engine Oil',                'Engine oil used at service',              'LTR',  'Lubricants & Fluids'),
    ('PART', 'Transmission Fluid',        'Gearbox / transmission fluid',            'LTR',  'Lubricants & Fluids'),
    ('PART', 'Coolant',                   'Engine coolant',                          'LTR',  'Lubricants & Fluids'),
    ('PART', 'Brake Fluid',               'Brake and clutch fluid',                  'LTR',  'Lubricants & Fluids'),
    ('PART', 'Oil Filter',                'Engine oil filter',                       'NOS',  'Filters'),
    ('PART', 'Air Filter',                'Engine air filter',                       'NOS',  'Filters'),
    ('PART', 'Cabin Filter',              'Cabin / AC filter',                       'NOS',  'Filters'),
    ('PART', 'Fuel Filter',               'Fuel filter',                             'NOS',  'Filters'),
    ('PART', 'Brake Pads',                'Brake pad set',                           'SET',  'Brakes'),
    ('PART', 'Brake Discs',               'Brake disc / rotor',                      'PAIR', 'Brakes'),
    ('PART', 'Spark Plug',                'Spark plug',                              'NOS',  'Ignition'),
    ('PART', 'Battery',                   'Vehicle battery',                         'NOS',  'Electrical'),
    ('PART', 'Headlamp Bulb',             'Headlamp bulb',                           'NOS',  'Electrical'),
    ('PART', 'Wiper Blade',               'Windshield wiper blade',                  'NOS',  'Body & Exterior'),
    ('PART', 'Drive Belt',                'Engine drive / timing belt',              'NOS',  'Engine'),
    ('PART', 'Fasteners (Nuts and Bolts)','Common workshop fasteners',               'KIT',  'Hardware'),

    ('SERVICE', 'General Periodic Service',  'Scheduled periodic service',           'JOB',  'Periodic Maintenance'),
    ('SERVICE', 'Engine Oil Replacement',    'Drain and refill engine oil',          'JOB',  'Periodic Maintenance'),
    ('SERVICE', 'Oil Filter Replacement',    'Replace the engine oil filter',        'JOB',  'Periodic Maintenance'),
    ('SERVICE', 'Air Filter Replacement',    'Replace the engine air filter',        'JOB',  'Periodic Maintenance'),
    ('SERVICE', 'Coolant Replacement',       'Drain and refill coolant',             'JOB',  'Periodic Maintenance'),
    ('SERVICE', 'Tyre Rotation',             'Rotate tyres',                         'JOB',  'Periodic Maintenance'),
    ('SERVICE', 'Brake Inspection',          'Inspect brake system',                 'JOB',  'Brakes'),
    ('SERVICE', 'Brake Pad Replacement',     'Replace brake pads',                   'JOB',  'Brakes'),
    ('SERVICE', 'Brake Fluid Replacement',   'Flush and refill brake fluid',         'JOB',  'Brakes'),
    ('SERVICE', 'Battery Testing',           'Test battery health',                  'JOB',  'Electrical'),
    ('SERVICE', 'Battery Replacement',       'Replace the vehicle battery',          'JOB',  'Electrical'),
    ('SERVICE', 'Electrical Diagnostics',    'Diagnose electrical faults',           'JOB',  'Electrical'),
    ('SERVICE', 'Engine Diagnostics',        'Scan and diagnose engine faults',      'JOB',  'Diagnostics'),
    ('SERVICE', 'Road Test and Inspection',  'Road test and general inspection',     'JOB',  'Diagnostics'),
    ('SERVICE', 'AC Inspection',             'Inspect the air-conditioning system',  'JOB',  'Air Conditioning'),
    ('SERVICE', 'AC Servicing',              'Service the air-conditioning system',  'JOB',  'Air Conditioning'),
    ('SERVICE', 'Wheel Alignment',           'Align wheels',                         'JOB',  'Wheels & Suspension'),
    ('SERVICE', 'Wheel Balancing',           'Balance wheels',                       'JOB',  'Wheels & Suspension'),
    ('SERVICE', 'Suspension Inspection',     'Inspect suspension',                   'JOB',  'Wheels & Suspension'),
    ('SERVICE', 'Clutch Inspection',         'Inspect the clutch',                   'JOB',  'Transmission')
) AS v(item_type, name, description, default_unit, category)
ON CONFLICT (item_type, name_normalized) DO NOTHING;
