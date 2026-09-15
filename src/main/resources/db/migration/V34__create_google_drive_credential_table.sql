CREATE TABLE google_drive_credential
(
    id BIGSERIAL PRIMARY KEY,

    credential_key VARCHAR(100) NOT NULL,

    access_token TEXT,

    refresh_token TEXT,

    expiration_time_millis BIGINT,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP
);

CREATE UNIQUE INDEX uk_google_drive_credential_key
ON google_drive_credential(credential_key);
