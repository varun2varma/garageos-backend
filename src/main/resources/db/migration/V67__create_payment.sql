--==========================================================
-- GarageOS - Payment records
-- V67
--
-- Invoice.payment_status stays the canonical status; this table is the
-- ledger of individual payments that produces it.
--==========================================================

CREATE TABLE payment (

    id BIGSERIAL PRIMARY KEY,

    invoice_id BIGINT NOT NULL,

    garage_id BIGINT NOT NULL,

    amount NUMERIC(12,2) NOT NULL,

    payment_method VARCHAR(30) NOT NULL,

    reference VARCHAR(100),

    received_by_user_id BIGINT,

    paid_at TIMESTAMP NOT NULL,

    status VARCHAR(30) NOT NULL,

    -- Client-supplied key making a retried submission idempotent.
    idempotency_key VARCHAR(80),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT fk_payment_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoice(id),

    CONSTRAINT fk_payment_garage
        FOREIGN KEY (garage_id) REFERENCES garage(id),

    CONSTRAINT ck_payment_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_payment_invoice ON payment(invoice_id);
CREATE INDEX idx_payment_garage_paid_at ON payment(garage_id, paid_at);
CREATE UNIQUE INDEX uk_payment_idempotency_key
    ON payment(idempotency_key) WHERE idempotency_key IS NOT NULL;

-- Preserve already-paid invoices as a single payment of the full total
-- (method was never captured, hence UNSPECIFIED).
INSERT INTO payment (invoice_id, garage_id, amount, payment_method, paid_at, status)
SELECT i.id, jc.garage_id, i.grand_total, 'UNSPECIFIED',
       COALESCE(i.updated_at, i.created_at), 'COMPLETED'
FROM invoice i
JOIN estimate e ON e.id = i.estimate_id
JOIN job_card jc ON jc.id = e.job_card_id
WHERE i.payment_status = 'PAID'
  AND jc.garage_id IS NOT NULL
  AND i.grand_total > 0;
