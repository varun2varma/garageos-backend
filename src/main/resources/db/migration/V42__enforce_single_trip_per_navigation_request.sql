--/*
--==========================================================
-- GarageOS - Navigation: enforce at most one NavigationTrip
-- per NavigationRequest at the database level.
--
-- NavigationTripServiceImpl.assignDriver() already guards this
-- in application code (rejects assignment unless the request's
-- status is REQUESTED, then flips it to ASSIGNED in the same
-- transaction) - this is defense-in-depth against the exact
-- race two concurrent "Assign Driver" submissions can hit
-- under the database's default (READ COMMITTED) isolation,
-- where both transactions can read status = REQUESTED before
-- either commits its update, each convinced assignment is
-- still available and creating a second trip for the same
-- request. This does not change existing behavior; it only
-- makes the invariant the service already assumes actually
-- unbreakable, does not touch any existing row, and matches
-- the same defense-in-depth pattern already used for
-- vehicle_handover (V39).
-- V42
--==========================================================
--*/

CREATE UNIQUE INDEX idx_navigation_trip_one_per_request
    ON navigation_trip (navigation_request_id);
