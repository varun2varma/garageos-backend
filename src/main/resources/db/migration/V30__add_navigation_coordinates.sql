ALTER TABLE navigation_request
    ADD COLUMN pickup_latitude NUMERIC(10,7),
    ADD COLUMN pickup_longitude NUMERIC(10,7),
    ADD COLUMN delivery_latitude NUMERIC(10,7),
    ADD COLUMN delivery_longitude NUMERIC(10,7);