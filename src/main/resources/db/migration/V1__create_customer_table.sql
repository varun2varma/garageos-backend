CREATE TABLE customer
(
    id BIGSERIAL PRIMARY KEY,

    first_name VARCHAR(100) NOT NULL,

    last_name VARCHAR(100),

    mobile_number VARCHAR(15) NOT NULL UNIQUE,

    email VARCHAR(150),

    address TEXT,

    city VARCHAR(100),

    state VARCHAR(100),

    pincode VARCHAR(10),

    remarks TEXT,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP
);

CREATE INDEX idx_customer_mobile
ON customer(mobile_number);

CREATE INDEX idx_customer_name
ON customer(first_name, last_name);