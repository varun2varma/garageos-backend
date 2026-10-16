-- GarageST - explicit customer-visibility flag on driver trip evidence.
--
-- Final reports previously treated trip evidence as customer-visible purely because of its stage.
-- Visibility is now an explicit, persisted decision: a trip photo appears in a final report (owner or
-- customer) only when customer_visible is TRUE. New uploads set it from the stage's default policy
-- (pickup and delivery handover evidence = visible); existing pickup/delivery rows keep their current
-- report behaviour via this backfill. No row is deleted or otherwise changed.

ALTER TABLE navigation_trip_media
    ADD COLUMN customer_visible BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE navigation_trip_media
   SET customer_visible = TRUE
 WHERE media_stage IN ('BEFORE_PICKUP', 'DELIVERY');
