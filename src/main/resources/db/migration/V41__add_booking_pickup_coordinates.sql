/*
==========================================================
 GarageOS - V41
 Booking pickup coordinates
==========================================================

 The booking table stored pickup location as free text only
 (pickup_address VARCHAR(500)). A typed address is descriptive metadata,
 not a navigable location - a driver sent to "Kukatpally" has nothing to
 navigate to.

 navigation_request already gained pickup_latitude / pickup_longitude in
 V30, but nothing upstream ever produced them: the columns had no fields
 on the NavigationRequest entity, so the values were dropped before they
 could be written. This migration supplies the missing upstream half, and
 the entity mapping is added alongside it.

 Latitude/longitude become the canonical navigation coordinates; the
 address remains human-readable metadata shown alongside them.

 Additive and nullable throughout:
   - every existing booking stays valid with both columns NULL
   - a booking without pickup never has coordinates
   - no existing column, index or constraint is altered
*/

ALTER TABLE booking
    ADD COLUMN pickup_latitude NUMERIC(10,7),
    ADD COLUMN pickup_longitude NUMERIC(10,7);

COMMENT ON COLUMN booking.pickup_latitude IS
    'Canonical pickup latitude. NULL when pickup was not requested, or for bookings created before V41.';

COMMENT ON COLUMN booking.pickup_longitude IS
    'Canonical pickup longitude. NULL when pickup was not requested, or for bookings created before V41.';
