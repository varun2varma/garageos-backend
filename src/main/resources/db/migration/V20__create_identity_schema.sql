/* ============================================================
   GarageOS - Identity & Access Management
   ============================================================ */

/* ============================================================
   Roles
   ============================================================ */

CREATE TABLE roles
(
    id BIGSERIAL PRIMARY KEY,

    code VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),

    system_role BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_roles_code
        UNIQUE (code)
);

CREATE INDEX idx_roles_code
ON roles(code);


/* ============================================================
   Permissions
   ============================================================ */

CREATE TABLE permissions
(
    id BIGSERIAL PRIMARY KEY,

    module VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,

    code VARCHAR(100) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    description VARCHAR(500),

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_permissions_code
        UNIQUE (code)
);

CREATE INDEX idx_permissions_module
ON permissions(module);

CREATE INDEX idx_permissions_action
ON permissions(action);

CREATE INDEX idx_permissions_code
ON permissions(code);


/* ============================================================
   Users
   ============================================================ */

CREATE TABLE users
(
    id BIGSERIAL PRIMARY KEY,

    garage_id BIGINT,

    employee_code VARCHAR(30),

    username VARCHAR(100) NOT NULL,

    password_hash VARCHAR(255) NOT NULL,

    first_name VARCHAR(100) NOT NULL,

    last_name VARCHAR(100),

    mobile VARCHAR(20),

    email VARCHAR(150),

    status VARCHAR(30) NOT NULL,

    authentication_provider VARCHAR(30) NOT NULL,

    first_login BOOLEAN NOT NULL DEFAULT TRUE,

    failed_attempts INTEGER NOT NULL DEFAULT 0,

    locked_until TIMESTAMP,

    last_login TIMESTAMP,

    password_changed_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_users_username
        UNIQUE(username),

    CONSTRAINT uk_users_email
        UNIQUE(email),

    CONSTRAINT uk_users_mobile
        UNIQUE(mobile)
);

CREATE INDEX idx_users_username
ON users(username);

CREATE INDEX idx_users_status
ON users(status);

CREATE INDEX idx_users_garage
ON users(garage_id);


/* ============================================================
   User Roles
   ============================================================ */

CREATE TABLE user_roles
(
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,

    role_id BIGINT NOT NULL,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY(user_id)
        REFERENCES users(id),

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY(role_id)
        REFERENCES roles(id),

    CONSTRAINT uk_user_role
        UNIQUE(user_id, role_id)
);

CREATE INDEX idx_user_roles_user
ON user_roles(user_id);

CREATE INDEX idx_user_roles_role
ON user_roles(role_id);


/* ============================================================
   Role Permissions
   ============================================================ */

CREATE TABLE role_permissions
(
    id BIGSERIAL PRIMARY KEY,

    role_id BIGINT NOT NULL,

    permission_id BIGINT NOT NULL,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY(role_id)
        REFERENCES roles(id),

    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY(permission_id)
        REFERENCES permissions(id),

    CONSTRAINT uk_role_permission
        UNIQUE(role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role
ON role_permissions(role_id);

CREATE INDEX idx_role_permissions_permission
ON role_permissions(permission_id);


/* ============================================================
   User Sessions
   ============================================================ */

CREATE TABLE user_sessions
(
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,

    refresh_token VARCHAR(500) NOT NULL,

    ip_address VARCHAR(100),

    user_agent VARCHAR(500),

    expires_at TIMESTAMP NOT NULL,

    revoked BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_user_sessions_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
);

CREATE INDEX idx_user_sessions_user
ON user_sessions(user_id);

CREATE INDEX idx_user_sessions_refresh_token
ON user_sessions(refresh_token);

CREATE INDEX idx_user_sessions_expires
ON user_sessions(expires_at);


/* ============================================================
   Seed Roles
   ============================================================ */

INSERT INTO roles
(
    code,
    display_name,
    description,
    system_role,
    created_at,
    updated_at
)
VALUES
(
    'SUPER_ADMIN',
    'Super Admin',
    'GarageOS Platform Administrator',
    TRUE,
    NOW(),
    NOW()
),
(
    'OWNER',
    'Owner',
    'Garage Owner',
    TRUE,
    NOW(),
    NOW()
),
(
    'MANAGER',
    'Manager',
    'Operations Manager',
    TRUE,
    NOW(),
    NOW()
),
(
    'SERVICE_ADVISOR',
    'Service Advisor',
    'Front Office Service Advisor',
    TRUE,
    NOW(),
    NOW()
),
(
    'TECHNICIAN',
    'Technician',
    'Garage Technician',
    TRUE,
    NOW(),
    NOW()
),
(
    'INVENTORY_MANAGER',
    'Inventory Manager',
    'Inventory & Purchase Manager',
    TRUE,
    NOW(),
    NOW()
),
(
    'ACCOUNTANT',
    'Accountant',
    'Finance & Accounts',
    TRUE,
    NOW(),
    NOW()
),
(
    'CASHIER',
    'Cashier',
    'Billing & Payment Collection',
    TRUE,
    NOW(),
    NOW()
),
(
    'CUSTOMER',
    'Customer',
    'Customer Portal User',
    TRUE,
    NOW(),
    NOW()
);