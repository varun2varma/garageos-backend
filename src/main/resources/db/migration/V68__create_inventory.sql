--==========================================================
-- GarageOS - Inventory MVP (parts catalogue + stock movements)
-- V68
--==========================================================

CREATE TABLE inventory_item (

    id BIGSERIAL PRIMARY KEY,

    garage_id BIGINT NOT NULL,

    name VARCHAR(150) NOT NULL,

    part_number VARCHAR(60),

    unit VARCHAR(20) NOT NULL,

    quantity NUMERIC(12,3) NOT NULL DEFAULT 0,

    reorder_level NUMERIC(12,3) NOT NULL DEFAULT 0,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT fk_inventory_item_garage
        FOREIGN KEY (garage_id) REFERENCES garage(id),

    CONSTRAINT ck_inventory_item_quantity CHECK (quantity >= 0),

    CONSTRAINT ck_inventory_item_reorder CHECK (reorder_level >= 0)
);

CREATE INDEX idx_inventory_item_garage ON inventory_item(garage_id);

CREATE UNIQUE INDEX uk_inventory_item_garage_part_number
    ON inventory_item(garage_id, part_number) WHERE part_number IS NOT NULL;

CREATE TABLE inventory_stock_movement (

    id BIGSERIAL PRIMARY KEY,

    item_id BIGINT NOT NULL,

    garage_id BIGINT NOT NULL,

    movement_type VARCHAR(20) NOT NULL,

    -- Signed delta actually applied to the item quantity.
    quantity NUMERIC(12,3) NOT NULL,

    balance_after NUMERIC(12,3) NOT NULL,

    reason VARCHAR(255),

    -- Set when stock is issued against a job card (consumption).
    job_card_id BIGINT,

    performed_by_user_id BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT fk_stock_movement_item
        FOREIGN KEY (item_id) REFERENCES inventory_item(id),

    CONSTRAINT fk_stock_movement_garage
        FOREIGN KEY (garage_id) REFERENCES garage(id),

    CONSTRAINT fk_stock_movement_job_card
        FOREIGN KEY (job_card_id) REFERENCES job_card(id),

    CONSTRAINT ck_stock_movement_nonzero CHECK (quantity <> 0)
);

CREATE INDEX idx_stock_movement_item ON inventory_stock_movement(item_id);
CREATE INDEX idx_stock_movement_garage_created
    ON inventory_stock_movement(garage_id, created_at);
CREATE INDEX idx_stock_movement_job_card ON inventory_stock_movement(job_card_id);
