--/*
--==========================================================
-- GarageOS - Vehicle Handover: enforce at most one PENDING
-- code per trip/direction at a time (the application already
-- expires the prior PENDING row before issuing a new one -
-- this is a defense-in-depth constraint, not a behavior
-- change, and does not touch any existing row).
-- V39
--==========================================================
--*/

CREATE UNIQUE INDEX idx_vehicle_handover_one_pending
    ON vehicle_handover (trip_id, direction)
    WHERE status = 'PENDING';
