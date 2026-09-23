--/*
--==========================================================
-- GarageOS - Customer Live Vehicle Journey
-- V45
--
-- Additive, non-destructive indexes only - no table/column changes.
-- Supports CustomerVehicleJourneyServiceImpl's batched, status-filtered
-- lookups by vehicle_id across booking/navigation_request/navigation_trip.
--
-- job_card(vehicle_id) already has an index from V3, so it is not
-- repeated here - a single-column index already covers the
-- findByVehicle_IdInAndStatusNotIn query's vehicle_id predicate well
-- enough for this feature's scale (a customer's own vehicle list).
--==========================================================
--*/

CREATE INDEX idx_booking_vehicle_status
    ON booking(vehicle_id, status);

CREATE INDEX idx_navigation_request_vehicle_status
    ON navigation_request(vehicle_id, status);

CREATE INDEX idx_navigation_trip_vehicle_status
    ON navigation_trip(vehicle_id, status);
