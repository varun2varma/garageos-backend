-- Garage bank account REGISTRATION for future payouts. This stores and tracks the account; it does not verify
-- ownership of it (that needs a bank-verification provider) and it does not move money.
--
--  * The account number is stored only encrypted (AES-GCM, application-level key from the environment); only the last
--    four digits are kept in clear for masked display.
--  * A garage has at most ONE current account (partial unique index); replacing it retires the old row instead of
--    overwriting it, so there is a change history.
--  * verification_status starts at PENDING_VERIFICATION and only a provider result can change it; nothing here is ever
--    marked VERIFIED by default.
-- Additive: nothing existing is altered.

CREATE TABLE garage_bank_account (
    id                     BIGSERIAL PRIMARY KEY,
    garage_id              BIGINT       NOT NULL,
    account_holder_name    VARCHAR(100) NOT NULL,
    bank_name              VARCHAR(100),
    ifsc                   VARCHAR(11)  NOT NULL,
    account_type           VARCHAR(10)  NOT NULL,
    account_number_enc     TEXT         NOT NULL,
    account_number_last4   VARCHAR(4)   NOT NULL,
    verification_status    VARCHAR(30)  NOT NULL,
    verification_provider  VARCHAR(40),
    verification_reference VARCHAR(100),
    verified_at            TIMESTAMP,
    failure_reason         VARCHAR(300),
    consent_at             TIMESTAMP    NOT NULL,
    is_current             BOOLEAN      NOT NULL,
    created_by_user_id     BIGINT       NOT NULL,
    superseded_at          TIMESTAMP,
    retired_by_user_id     BIGINT,
    created_at             TIMESTAMP    NOT NULL,
    updated_at             TIMESTAMP,

    CONSTRAINT fk_bank_account_garage FOREIGN KEY (garage_id) REFERENCES garage(id),
    CONSTRAINT ck_bank_account_ifsc CHECK (ifsc ~ '^[A-Z]{4}0[A-Z0-9]{6}$'),
    CONSTRAINT ck_bank_account_last4 CHECK (account_number_last4 ~ '^[0-9]{4}$'),
    CONSTRAINT ck_bank_account_type CHECK (account_type IN ('SAVINGS', 'CURRENT')),
    CONSTRAINT ck_bank_account_status CHECK (verification_status IN
        ('PENDING_VERIFICATION', 'VERIFIED', 'FAILED', 'NEEDS_REVIEW'))
);

CREATE UNIQUE INDEX uk_bank_account_one_current_per_garage ON garage_bank_account (garage_id) WHERE is_current;
CREATE INDEX idx_bank_account_garage ON garage_bank_account (garage_id, created_at DESC);
