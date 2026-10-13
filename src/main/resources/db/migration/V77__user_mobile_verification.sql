-- V77: record WHICH mobile number a user has proven they control (by completing an SMS OTP), and when.
-- Password recovery by SMS may only target a verified number. A stored mobile is not proof of ownership
-- (staff accounts are created by an administrator; accounts registered while OTP was switched off never proved it).
--
-- Columns are nullable and are NOT blanket-filled. The backfill below marks a user verified only where the
-- existing otp_challenge history proves it for that exact number:
--   * USER_REGISTRATION: a VERIFIED challenge for this very number that completed no earlier than 1 hour before
--     the account was created (registration consumes the OTP in the same request that creates the user);
--   * GARAGE_REGISTRATION: a VERIFIED challenge issued for this user id and this number.
-- Everyone else stays unverified and can verify through POST /auth/mobile/verify (purpose MOBILE_VERIFICATION).
ALTER TABLE users
    ADD COLUMN mobile_verified_at     TIMESTAMP,
    ADD COLUMN mobile_verified_number VARCHAR(20);

UPDATE users u
   SET mobile_verified_at = c.verified_at,
       mobile_verified_number = u.mobile
  FROM (
        SELECT DISTINCT ON (u2.id) u2.id AS user_id, oc.verified_at
          FROM users u2
          JOIN otp_challenge oc
            ON oc.destination = u2.mobile
           AND oc.status = 'VERIFIED'
           AND oc.verified_at IS NOT NULL
           AND (
                 (oc.purpose = 'USER_REGISTRATION'
                    AND oc.verified_at <= u2.created_at + INTERVAL '5 minutes'
                    AND oc.verified_at >= u2.created_at - INTERVAL '1 hour')
              OR (oc.purpose IN ('GARAGE_REGISTRATION', 'MOBILE_VERIFICATION') AND oc.user_id = u2.id)
               )
         WHERE u2.mobile IS NOT NULL
         ORDER BY u2.id, oc.verified_at DESC
       ) c
 WHERE u.id = c.user_id
   AND u.mobile_verified_at IS NULL;
