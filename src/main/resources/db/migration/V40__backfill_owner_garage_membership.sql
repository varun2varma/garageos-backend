/*
==========================================================
 GarageOS - V40
 Backfill OWNER garage memberships
==========================================================

 Corrective data fix.

 GarageServiceImpl.createGarage() set users.garage_id and granted the
 OWNER role, but never inserted a garage_membership row. Every
 garage-context consumer in the system resolves context from
 garage_membership:

   - GET /garage-memberships/my
   - dashboard + job card "allGarages" aggregation
   - the exists(garage_id, user_id) validation used whenever a caller
     supplies a garageId

 so every owner registered before this release has an empty membership
 list and hits "No garage context for this account."

 The service now creates the membership on registration; this migration
 repairs the owners who already exist.

 Additive and idempotent:
   - inserts only where no membership row exists for that (garage, user)
   - respects uk_garage_membership(garage_id, user_id)
   - touches no other membership, and changes no existing row

 garage_membership_role is deliberately left alone: no code in this
 codebase has ever written to it, and membership roles are read from
 user_roles (see GarageMembershipServiceImpl.buildResponse).
*/

---------------------------------------------------------------
-- 1. Owner memberships
---------------------------------------------------------------

-- An owner is a user who holds the OWNER role and is pointed at a
-- garage. Their membership is ACTIVE and self-approved: an owner does
-- not apply to their own garage and has nobody else to approve them.
INSERT INTO garage_membership (
    garage_id,
    user_id,
    status,
    joined_at,
    approved_at,
    approved_by,
    created_at,
    updated_at
)
-- DISTINCT guards the (garage_id, user_id) unique index: NOT EXISTS is
-- evaluated against the pre-statement snapshot, so a user carrying a
-- duplicate OWNER role row would otherwise produce two identical inserts
-- in this one statement and violate uk_garage_membership.
SELECT DISTINCT
    u.garage_id,
    u.id,
    'ACTIVE',
    COALESCE(u.created_at, CURRENT_TIMESTAMP),
    COALESCE(u.created_at, CURRENT_TIMESTAMP),
    u.id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE u.garage_id IS NOT NULL
  AND r.code = 'OWNER'
  AND NOT EXISTS (
      SELECT 1
      FROM garage_membership gm
      WHERE gm.garage_id = u.garage_id
        AND gm.user_id = u.id
  );

---------------------------------------------------------------
-- 2. Garage owner back-reference
---------------------------------------------------------------

-- garage.owner_user_id was added in V24 but never populated, because
-- nothing ever wrote it. Fill it in from the same OWNER relationship so
-- the column stops being permanently null.
UPDATE garage g
SET owner_user_id = u.id
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE u.garage_id = g.id
  AND r.code = 'OWNER'
  AND g.owner_user_id IS NULL;
