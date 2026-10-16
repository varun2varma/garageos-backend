-- GarageST - Owner-authorized credit on an invoice (separate from payment).
--
-- A credit authorization is a FINANCIAL AUTHORIZATION that lets a vehicle be released while an invoice is
-- unpaid. It never changes invoice.payment_status and never makes the invoice PDF available (PAID only).
-- Repayments are ordinary rows in `payment`; when the invoice becomes fully paid the ACTIVE authorization is
-- moved to SETTLED in the same transaction.
--
-- No customer credit limit or maximum term is invented here (no approved business rule exists yet): the
-- service enforces amount <= outstanding balance and a mandatory future due date.

CREATE TABLE invoice_credit_authorization (
    id BIGSERIAL PRIMARY KEY,
    garage_id BIGINT NOT NULL,
    invoice_id BIGINT NOT NULL,
    job_card_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    authorized_amount NUMERIC(12, 2) NOT NULL,
    due_date DATE NOT NULL,
    reason VARCHAR(500) NOT NULL,
    authorized_by_user_id BIGINT NOT NULL,
    authorized_at TIMESTAMP NOT NULL,
    revoked_by_user_id BIGINT,
    revoked_at TIMESTAMP,
    revoke_reason VARCHAR(500),
    settled_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_credit_invoice FOREIGN KEY (invoice_id) REFERENCES invoice (id),
    CONSTRAINT fk_credit_job_card FOREIGN KEY (job_card_id) REFERENCES job_card (id),
    CONSTRAINT ck_credit_status CHECK (status IN ('ACTIVE', 'REVOKED', 'SETTLED')),
    CONSTRAINT ck_credit_amount_positive CHECK (authorized_amount > 0)
);

-- at most ONE active authorization per invoice, even under concurrent requests
CREATE UNIQUE INDEX uk_credit_one_active_per_invoice
    ON invoice_credit_authorization (invoice_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_credit_garage_status ON invoice_credit_authorization (garage_id, status);
CREATE INDEX idx_credit_invoice ON invoice_credit_authorization (invoice_id);
