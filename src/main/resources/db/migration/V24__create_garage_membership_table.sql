---------------------------------------------------------------
-- Garage Owner
---------------------------------------------------------------

ALTER TABLE garage
ADD COLUMN owner_user_id BIGINT;

ALTER TABLE garage
ADD CONSTRAINT fk_garage_owner
FOREIGN KEY (owner_user_id)
REFERENCES users(id);

CREATE INDEX idx_garage_owner
ON garage(owner_user_id);

---------------------------------------------------------------
--   GARAGEOS V1
--   Garage Membership
---------------------------------------------------------------

CREATE TABLE garage_membership
(
    id BIGSERIAL PRIMARY KEY,

    garage_id BIGINT NOT NULL,

    user_id BIGINT NOT NULL,

    employee_code VARCHAR(30),

    status VARCHAR(30) NOT NULL,

    joined_at TIMESTAMP,

    approved_at TIMESTAMP,

    approved_by BIGINT,

    remarks VARCHAR(500),

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP,

    created_by VARCHAR(100),

    updated_by VARCHAR(100),

    CONSTRAINT fk_garage_membership_garage
        FOREIGN KEY (garage_id)
            REFERENCES garage(id),

    CONSTRAINT fk_garage_membership_user
        FOREIGN KEY (user_id)
            REFERENCES users(id),

    CONSTRAINT fk_garage_membership_approved_by
        FOREIGN KEY (approved_by)
            REFERENCES users(id)
);

CREATE INDEX idx_garage_membership_garage
ON garage_membership(garage_id);

CREATE INDEX idx_garage_membership_user
ON garage_membership(user_id);

CREATE INDEX idx_garage_membership_status
ON garage_membership(status);

CREATE UNIQUE INDEX uk_garage_membership
ON garage_membership(garage_id, user_id);

---------------------------------------------------------------
-- Garage Membership Role
---------------------------------------------------------------

CREATE TABLE garage_membership_role
(
    id BIGSERIAL PRIMARY KEY,

    membership_id BIGINT NOT NULL,

    role_id BIGINT NOT NULL,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP,

    created_by VARCHAR(100),

    updated_by VARCHAR(100),

    CONSTRAINT fk_membership_role_membership
        FOREIGN KEY (membership_id)
            REFERENCES garage_membership(id),

    CONSTRAINT fk_membership_role_role
        FOREIGN KEY (role_id)
            REFERENCES roles(id)
);

CREATE INDEX idx_membership_role_membership
ON garage_membership_role(membership_id);

CREATE INDEX idx_membership_role_role
ON garage_membership_role(role_id);

CREATE UNIQUE INDEX uk_membership_role
ON garage_membership_role(membership_id, role_id);