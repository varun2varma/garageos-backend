-- V75: the customer choice of how the finished vehicle is returned (collect at garage vs driver delivery).
-- One row per job card; additive, no existing table is altered. A driver-delivery choice is mirrored into the
-- existing navigation_request (DELIVERY) rather than duplicating dispatch state.
CREATE TABLE job_card_return_preference (
    id                 BIGSERIAL PRIMARY KEY,
    job_card_id        BIGINT       NOT NULL,
    customer_id        BIGINT       NOT NULL,
    method             VARCHAR(30)  NOT NULL,
    delivery_address   VARCHAR(500),
    delivery_latitude  NUMERIC(10, 7),
    delivery_longitude NUMERIC(10, 7),
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL,
    CONSTRAINT uk_return_preference_job_card UNIQUE (job_card_id),
    CONSTRAINT fk_return_preference_job_card FOREIGN KEY (job_card_id) REFERENCES job_card (id),
    CONSTRAINT fk_return_preference_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT ck_return_preference_method CHECK (method IN ('COLLECT_AT_GARAGE', 'DRIVER_DELIVERY'))
);
