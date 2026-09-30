-- GarageOS - Navigation: direct reference from a NavigationRequest to the
-- JobCard it belongs to, so the service journey never has to be guessed from
-- vehicle_id alone.
--   * DELIVERY requests carry the JobCard being delivered (required for new
--     delivery requests by NavigationRequestServiceImpl).
--   * PICKUP requests get the JobCard that was created from their Booking
--     (set by JobCardServiceImpl when the JobCard is created from a booking).
-- Nullable: every pre-existing request stays valid with NULL.

ALTER TABLE navigation_request
    ADD COLUMN job_card_id BIGINT;

CREATE INDEX idx_navigation_request_job_card
    ON navigation_request (job_card_id);

-- At most one non-cancelled DELIVERY request per JobCard (defense in depth for
-- the check in NavigationRequestServiceImpl).
CREATE UNIQUE INDEX idx_navigation_request_one_delivery_per_job
    ON navigation_request (job_card_id)
    WHERE request_type = 'DELIVERY' AND status <> 'CANCELLED' AND job_card_id IS NOT NULL;
