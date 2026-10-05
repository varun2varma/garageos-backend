--==========================================================
-- GarageOS - Customer <-> Garage relationship metadata
-- V66
--
-- Relationship metadata only. JobCard / Booking remain the source of
-- truth for service history; this table records WHICH garages a customer
-- has a verified relationship with, plus the customer's default garage.
--==========================================================

CREATE TABLE customer_garage (

    id BIGSERIAL PRIMARY KEY,

    customer_id BIGINT NOT NULL,

    garage_id BIGINT NOT NULL,

    -- JOB_CARD / BOOKING_CONFIRMED count as a service relationship
    -- ("My Garages"). STAFF_REGISTERED only makes the customer visible to
    -- that garage's staff (customer onboarding) and is NOT shown in My Garages.
    source VARCHAR(30) NOT NULL,

    is_default BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT fk_customer_garage_customer
        FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,

    CONSTRAINT fk_customer_garage_garage
        FOREIGN KEY (garage_id) REFERENCES garage(id) ON DELETE CASCADE,

    CONSTRAINT uk_customer_garage UNIQUE (customer_id, garage_id)
);

CREATE INDEX idx_customer_garage_garage ON customer_garage(garage_id);

-- Only one default garage per customer.
CREATE UNIQUE INDEX uk_customer_garage_default
    ON customer_garage(customer_id) WHERE is_default;

-- Preserve existing implicit relationships.
INSERT INTO customer_garage (customer_id, garage_id, source)
SELECT DISTINCT jc.customer_id, jc.garage_id, 'JOB_CARD'
FROM job_card jc
WHERE jc.garage_id IS NOT NULL;

INSERT INTO customer_garage (customer_id, garage_id, source)
SELECT DISTINCT b.customer_id, b.garage_id, 'BOOKING_CONFIRMED'
FROM booking b
WHERE b.status IN ('CONFIRMED', 'COMPLETED')
  AND NOT EXISTS (
      SELECT 1 FROM customer_garage cg
      WHERE cg.customer_id = b.customer_id AND cg.garage_id = b.garage_id);
