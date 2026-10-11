--==========================================================
-- GarageST - Central parts/services catalog, GST rules,
-- estimate-line provenance and invoice tax snapshots
-- V69 (forward-only; every new column on an existing table is
-- nullable or defaulted so existing rows stay valid and unchanged)
--==========================================================

-- ---------------------------------------------------------
-- 1. Effective-dated GST rules (no rates are seeded here)
-- ---------------------------------------------------------
CREATE TABLE gst_tax_rule (

    id BIGSERIAL PRIMARY KEY,

    rule_code VARCHAR(40) NOT NULL,

    description VARCHAR(255),

    rate_percent NUMERIC(5,2) NOT NULL,

    effective_from DATE NOT NULL,

    effective_to DATE,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT ck_gst_tax_rule_rate CHECK (rate_percent >= 0 AND rate_percent <= 100),

    CONSTRAINT ck_gst_tax_rule_dates CHECK (effective_to IS NULL OR effective_to >= effective_from),

    CONSTRAINT uk_gst_tax_rule_code_from UNIQUE (rule_code, effective_from)
);

CREATE INDEX idx_gst_tax_rule_code ON gst_tax_rule(rule_code, effective_from);

-- ---------------------------------------------------------
-- 2. Central catalog (parts and services share one table)
-- ---------------------------------------------------------
CREATE TABLE catalog_item (

    id BIGSERIAL PRIMARY KEY,

    item_type VARCHAR(20) NOT NULL,

    name VARCHAR(150) NOT NULL,

    name_normalized VARCHAR(150) NOT NULL,

    description VARCHAR(500),

    default_unit VARCHAR(20) NOT NULL,

    -- HSN for PART, SAC for SERVICE; NULL while unclassified.
    hsn_sac_code VARCHAR(10),

    classification_status VARCHAR(20) NOT NULL DEFAULT 'UNCLASSIFIED',

    -- Logical reference to gst_tax_rule.rule_code (rules are effective-dated).
    tax_rule_code VARCHAR(40),

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_by_user_id BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT ck_catalog_item_type CHECK (item_type IN ('PART', 'SERVICE')),

    CONSTRAINT ck_catalog_item_status
        CHECK (classification_status IN ('UNCLASSIFIED', 'PENDING_REVIEW', 'VERIFIED')),

    CONSTRAINT ck_catalog_item_code_format
        CHECK (hsn_sac_code IS NULL OR hsn_sac_code ~ '^[0-9]{4,8}$'),

    -- A verified item must carry its classification and tax rule.
    CONSTRAINT ck_catalog_item_verified
        CHECK (classification_status <> 'VERIFIED'
               OR (hsn_sac_code IS NOT NULL AND tax_rule_code IS NOT NULL))
);

CREATE UNIQUE INDEX uk_catalog_item_type_name
    ON catalog_item(item_type, name_normalized);

CREATE INDEX idx_catalog_item_search
    ON catalog_item(item_type, active, name_normalized varchar_pattern_ops);

-- ---------------------------------------------------------
-- 3. Estimate line: catalog link, provenance, snapshots
-- ---------------------------------------------------------
ALTER TABLE estimate_item
    ADD COLUMN catalog_item_id BIGINT,
    ADD COLUMN garage_id BIGINT,
    ADD COLUMN unit VARCHAR(20),
    ADD COLUMN entry_source VARCHAR(20) NOT NULL DEFAULT 'LEGACY',
    ADD COLUMN review_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED',
    ADD COLUMN created_by_user_id BIGINT,
    ADD COLUMN manual_note VARCHAR(500),
    ADD COLUMN hsn_sac_code VARCHAR(10),
    ADD COLUMN tax_rule_code VARCHAR(40),
    ADD COLUMN gst_rate_percent NUMERIC(5,2);

ALTER TABLE estimate_item
    ADD CONSTRAINT fk_estimate_item_catalog
        FOREIGN KEY (catalog_item_id) REFERENCES catalog_item(id),
    ADD CONSTRAINT fk_estimate_item_garage
        FOREIGN KEY (garage_id) REFERENCES garage(id),
    ADD CONSTRAINT ck_estimate_item_entry_source
        CHECK (entry_source IN ('LEGACY', 'MASTER', 'MANUAL')),
    ADD CONSTRAINT ck_estimate_item_review_status
        CHECK (review_status IN ('NOT_REQUIRED', 'NEEDS_REVIEW', 'RESOLVED')),
    -- MASTER lines must reference the catalog; MANUAL lines must not.
    ADD CONSTRAINT ck_estimate_item_source_catalog
        CHECK ((entry_source = 'MASTER' AND catalog_item_id IS NOT NULL)
               OR (entry_source = 'MANUAL')
               OR (entry_source = 'LEGACY' AND catalog_item_id IS NULL));

-- Provenance garage for existing rows comes from the owning job card.
UPDATE estimate_item ei
SET garage_id = jc.garage_id
FROM estimate e
JOIN job_card jc ON jc.id = e.job_card_id
WHERE e.id = ei.estimate_id
  AND ei.garage_id IS NULL;

CREATE INDEX idx_estimate_item_catalog ON estimate_item(catalog_item_id);

CREATE INDEX idx_estimate_item_review
    ON estimate_item(review_status, garage_id)
    WHERE review_status = 'NEEDS_REVIEW';

-- ---------------------------------------------------------
-- 4. Invoice line: immutable tax snapshot (NULL = legacy line)
-- ---------------------------------------------------------
ALTER TABLE invoice_item
    ADD COLUMN catalog_item_id BIGINT,
    ADD COLUMN unit VARCHAR(20),
    ADD COLUMN hsn_sac_code VARCHAR(10),
    ADD COLUMN tax_rule_code VARCHAR(40),
    ADD COLUMN price_tax_mode VARCHAR(20),
    ADD COLUMN tax_source VARCHAR(30),
    ADD COLUMN gst_rate_percent NUMERIC(5,2),
    ADD COLUMN taxable_value NUMERIC(12,2),
    ADD COLUMN cgst_amount NUMERIC(12,2),
    ADD COLUMN sgst_amount NUMERIC(12,2),
    ADD COLUMN igst_amount NUMERIC(12,2),
    ADD COLUMN tax_amount NUMERIC(12,2),
    ADD COLUMN line_total NUMERIC(12,2);

ALTER TABLE invoice_item
    ADD CONSTRAINT fk_invoice_item_catalog
        FOREIGN KEY (catalog_item_id) REFERENCES catalog_item(id),
    ADD CONSTRAINT ck_invoice_item_price_tax_mode
        CHECK (price_tax_mode IS NULL OR price_tax_mode IN ('INCLUSIVE', 'EXCLUSIVE', 'NO_TAX'));

CREATE INDEX idx_invoice_item_catalog ON invoice_item(catalog_item_id);

-- ---------------------------------------------------------
-- 5. Invoice header: canonical tax totals (NULL = legacy invoice)
--    invoice.gst keeps its legacy meaning (GST added on top of
--    tax-exclusive lines); tax_total is the total GST.
-- ---------------------------------------------------------
ALTER TABLE invoice
    ADD COLUMN tax_snapshot BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN tax_total NUMERIC(12,2),
    ADD COLUMN cgst_total NUMERIC(12,2),
    ADD COLUMN sgst_total NUMERIC(12,2),
    ADD COLUMN igst_total NUMERIC(12,2),
    ADD COLUMN round_off NUMERIC(12,2),
    ADD COLUMN supply_type VARCHAR(20),
    ADD COLUMN supplier_gstin VARCHAR(15),
    ADD COLUMN supplier_state VARCHAR(100),
    ADD COLUMN place_of_supply_state VARCHAR(100);

ALTER TABLE invoice
    ADD CONSTRAINT ck_invoice_supply_type
        CHECK (supply_type IS NULL OR supply_type IN ('INTRA_STATE', 'INTER_STATE'));

-- ---------------------------------------------------------
-- 6. Optional garage inventory -> catalog link
-- ---------------------------------------------------------
ALTER TABLE inventory_item
    ADD COLUMN catalog_item_id BIGINT;

ALTER TABLE inventory_item
    ADD CONSTRAINT fk_inventory_item_catalog
        FOREIGN KEY (catalog_item_id) REFERENCES catalog_item(id);

CREATE INDEX idx_inventory_item_catalog ON inventory_item(catalog_item_id);
