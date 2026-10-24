-- Proof that a user controls the e-mail address on their account (e-mail OTP, purpose EMAIL_VERIFICATION).
-- Mirrors the mobile verification columns (V77): the verified ADDRESS is recorded, so a later change of e-mail
-- automatically means "not verified" without any extra bookkeeping. Existing users are simply unverified.
-- Additive and nullable.

ALTER TABLE users
    ADD COLUMN email_verified_at      TIMESTAMP,
    ADD COLUMN email_verified_address VARCHAR(150);
