package com.garageos.modules.media.entity;

import com.garageos.core.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Database-backed persistence for the Google Drive OAuth
 * {@code StoredCredential} the Google API client library needs to
 * automatically refresh the Drive access token (see
 * GoogleDriveCredentialDataStore). Deliberately holds only the three data
 * fields {@code StoredCredential} itself has — access token, refresh
 * token, expiration timestamp — nothing else about the OAuth flow.
 *
 * Replaces the previous local-disk FileDataStoreFactory
 * (google-drive-tokens/), which did not survive a Render restart/redeploy.
 * Treat the columns here as secret-equivalent: never log their values.
 */
@Entity
@Table(name = "google_drive_credential")
@Getter
@Setter
public class GoogleDriveCredential extends BaseEntity {

    @Column(name = "credential_key", nullable = false, unique = true, length = 100)
    private String credentialKey;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "expiration_time_millis")
    private Long expirationTimeMillis;
}
