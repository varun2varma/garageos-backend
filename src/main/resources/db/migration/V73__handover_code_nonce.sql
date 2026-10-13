-- V73: stable handover codes. The 6-digit code is re-derivable by the server (HMAC of this random per-row nonce under a
-- server secret) so reopening the screen shows the SAME code until it expires, without ever persisting the plaintext.
-- Nullable: rows issued before this migration have no nonce and are simply never re-shown (a new code is issued instead).
ALTER TABLE vehicle_handover
    ADD COLUMN code_nonce VARCHAR(64);

-- Retire any historical duplicate PENDING rows (keep the newest per trip) so the uniqueness guarantee below can be created.
UPDATE vehicle_handover h
   SET status = 'EXPIRED'
 WHERE h.status = 'PENDING'
   AND EXISTS (SELECT 1 FROM vehicle_handover n
                WHERE n.trip_id = h.trip_id AND n.status = 'PENDING' AND n.id > h.id);

-- At most one live (PENDING) code per trip, enforced by the database even under concurrent requests.
CREATE UNIQUE INDEX uk_vehicle_handover_one_pending
    ON vehicle_handover (trip_id)
    WHERE status = 'PENDING';
