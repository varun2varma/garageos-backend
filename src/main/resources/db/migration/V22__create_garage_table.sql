CREATE TABLE garage
(
    id BIGSERIAL PRIMARY KEY,

    garage_code VARCHAR(10) NOT NULL UNIQUE,

    garage_name VARCHAR(150) NOT NULL,

    workshop_type VARCHAR(30) NOT NULL,

    number_of_bays INTEGER NOT NULL,

    address VARCHAR(300),

    landmark VARCHAR(100),

    city VARCHAR(100),

    state VARCHAR(100),

    pincode VARCHAR(6),

    gst_number VARCHAR(15),

    pan_number VARCHAR(10),

    status VARCHAR(20) NOT NULL,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_garage_name
    ON garage(garage_name);

CREATE INDEX idx_garage_code
    ON garage(garage_code);