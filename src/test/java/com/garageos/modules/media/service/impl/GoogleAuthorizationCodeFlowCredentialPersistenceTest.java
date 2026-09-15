package com.garageos.modules.media.service.impl;

import com.garageos.modules.media.entity.GoogleDriveCredential;
import com.garageos.modules.media.repository.GoogleDriveCredentialRepository;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.DriveScopes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Verifies that Google's own {@code GoogleAuthorizationCodeFlow} correctly
 * drives {@link GoogleDriveCredentialDataStore} for the two operations
 * {@link com.garageos.modules.media.service.GoogleDriveOAuthService}
 * actually relies on:
 *   - createAndStoreCredential(tokenResponse, key) — what exchangeCode()
 *     calls once the (real, network-obtained) authorization code has
 *     already been exchanged for a token response;
 *   - a fresh DataStore instance (simulating a different application
 *     instance / a restart) reading back what an earlier instance wrote.
 *
 * Builds the flow the same way GoogleDriveOAuthService.createFlow() does
 * (transport, json factory, offline access, Drive scope, our DataStore) —
 * with fake, non-secret client id/secret, since none of this makes a real
 * network call: createAndStoreCredential only stores a TokenResponse that's
 * already been constructed in-memory here, it doesn't fetch one.
 */
@ExtendWith(MockitoExtension.class)
class GoogleAuthorizationCodeFlowCredentialPersistenceTest {

    private static final String KEY = "garagest-drive";

    @Mock
    private GoogleDriveCredentialRepository repository;

    private GoogleAuthorizationCodeFlow buildFlow(GoogleDriveCredentialDataStore dataStore) throws Exception {
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                .setWeb(new GoogleClientSecrets.Details()
                        .setClientId("fake-client-id-for-tests-only")
                        .setClientSecret("fake-client-secret-for-tests-only"));

        return new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                clientSecrets,
                Collections.singleton(DriveScopes.DRIVE_FILE)
        )
                .setCredentialDataStore(dataStore)
                .setAccessType("offline")
                .build();
    }

    // ------------------------------------------------------------
    // 3. Authorization-code exchange -> credential is persisted.
    // ------------------------------------------------------------
    @Test
    void createAndStoreCredential_persistsViaOurDataStore() throws Exception {
        when(repository.findByCredentialKey(KEY)).thenReturn(Optional.empty());

        GoogleDriveCredentialDataStore dataStore = new GoogleDriveCredentialDataStore(repository);
        GoogleAuthorizationCodeFlow flow = buildFlow(dataStore);

        GoogleTokenResponse tokenResponse = new GoogleTokenResponse();
        tokenResponse.setAccessToken("access-from-exchange");
        tokenResponse.setRefreshToken("refresh-from-exchange");
        tokenResponse.setExpiresInSeconds(3600L);

        flow.createAndStoreCredential(tokenResponse, KEY);

        ArgumentCaptor<GoogleDriveCredential> saved = ArgumentCaptor.forClass(GoogleDriveCredential.class);
        org.mockito.Mockito.verify(repository).save(saved.capture());
        assertThat(saved.getValue().getCredentialKey()).isEqualTo(KEY);
        assertThat(saved.getValue().getAccessToken()).isEqualTo("access-from-exchange");
        assertThat(saved.getValue().getRefreshToken()).isEqualTo("refresh-from-exchange");
    }

    // ------------------------------------------------------------
    // 4. Persisted refresh token survives "restart" semantics: a
    //    brand new GoogleDriveCredentialDataStore instance (no shared
    //    in-memory state with whatever wrote it — the only thing they
    //    share is the repository, standing in for the real shared
    //    database) can still load the credential and use it to build
    //    a working Credential object.
    // ------------------------------------------------------------
    @Test
    void freshDataStoreInstance_loadsCredentialPersistedEarlier() throws Exception {
        GoogleDriveCredential row = new GoogleDriveCredential();
        row.setCredentialKey(KEY);
        row.setAccessToken("access-before-restart");
        row.setRefreshToken("refresh-before-restart");
        row.setExpirationTimeMillis(System.currentTimeMillis() + 3_600_000);
        when(repository.findByCredentialKey(KEY)).thenReturn(Optional.of(row));

        // A brand new instance — nothing here was constructed by, or
        // shares memory with, whatever wrote the row above.
        GoogleDriveCredentialDataStore freshDataStore = new GoogleDriveCredentialDataStore(repository);
        GoogleAuthorizationCodeFlow freshFlow = buildFlow(freshDataStore);

        Credential loaded = freshFlow.loadCredential(KEY);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getAccessToken()).isEqualTo("access-before-restart");
        assertThat(loaded.getRefreshToken())
                .as("the refresh token must survive across instances/restarts for automatic re-authorization")
                .isEqualTo("refresh-before-restart");
    }
}
