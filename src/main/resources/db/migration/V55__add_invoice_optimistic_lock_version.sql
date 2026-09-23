-- Customer-payment-authorization investigation: receivePayment() has a
-- same-transaction "already PAID" guard, but no row-level protection
-- against two concurrent payment requests both reading PENDING before
-- either commits (Invoice has no @Version, unlike a properly-guarded
-- read-then-write flow). This closes that race with standard JPA/
-- Hibernate optimistic locking, scoped to Invoice only.
ALTER TABLE invoice
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
