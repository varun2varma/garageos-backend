package com.garageos.modules.media.service.impl;

import com.garageos.modules.media.entity.GoogleDriveCredential;
import com.garageos.modules.media.repository.GoogleDriveCredentialRepository;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PostgreSQL-backed {@link DataStore} for the single {@link StoredCredential}
 * the Google Drive OAuth flow needs (key {@code "garagest-drive"}). Passed
 * directly to {@code GoogleAuthorizationCodeFlow.Builder.setCredentialDataStore}
 * in place of the previous {@code FileDataStoreFactory}
 * (google-drive-tokens/), which did not survive a Render restart/redeploy.
 *
 * The Google API client library calls {@link #get} to load a stored
 * credential and {@link #set} both when a new authorization code is first
 * exchanged (GoogleDriveOAuthService.exchangeCode) and automatically every
 * time the library refreshes an expired access token
 * (DataStoreCredentialRefreshListener, wired in internally by the flow) —
 * this class does not need to (and does not) implement any refresh logic
 * itself, only correct, durable storage.
 *
 * SECURITY: every field of {@link StoredCredential} (access token, refresh
 * token, expiration) is treated as secret-equivalent. Nothing here ever
 * logs a token value — only row/key presence and counts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleDriveCredentialDataStore implements DataStore<StoredCredential> {

    private static final String STORE_ID = "GoogleDriveCredentialDataStore";

    private final GoogleDriveCredentialRepository repository;

    @Override
    public String getId() {
        return STORE_ID;
    }

    @Override
    public DataStoreFactory getDataStoreFactory() {
        // Not backed by a DataStoreFactory — this store is wired directly
        // into the flow via setCredentialDataStore(). Nothing in the OAuth
        // flow this service uses (loadCredential / createAndStoreCredential
        // / automatic refresh) calls this accessor.
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public int size() {
        return (int) repository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmpty() {
        return repository.count() == 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean containsKey(String key) {
        return repository.findByCredentialKey(key).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean containsValue(StoredCredential value) {
        if (value == null) {
            return false;
        }
        return repository.findAll().stream().anyMatch(row -> matches(row, value));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> keySet() {
        return repository.findAll().stream()
                .map(GoogleDriveCredential::getCredentialKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<StoredCredential> values() {
        return repository.findAll().stream()
                .map(this::toStoredCredential)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StoredCredential get(String key) {

        log.debug("[DRIVE_AUTH_STORE] Loading credential row. key={}", key);

        return repository.findByCredentialKey(key)
                .map(this::toStoredCredential)
                .orElse(null);
    }

    @Override
    @Transactional
    public DataStore<StoredCredential> set(String key, StoredCredential credential) throws IOException {

        if (key == null) {
            throw new IOException("Credential key is required.");
        }
        if (credential == null) {
            throw new IOException("Credential value is required.");
        }

        log.debug("[DRIVE_AUTH_STORE] Persisting credential row. key={}", key);

        GoogleDriveCredential existing = repository.findByCredentialKey(key).orElse(null);

        // Google's token-refresh response frequently omits a refresh token
        // (Google only issues a new one on first authorization) — the
        // Credential/DataStoreCredentialRefreshListener machinery already
        // preserves the in-memory refresh token in that case, but this is
        // a deliberate second safeguard at the persistence layer: never
        // let a null incoming refresh token blank out a previously stored,
        // still-valid one.
        String refreshTokenToPersist = credential.getRefreshToken();
        if (refreshTokenToPersist == null && existing != null) {
            refreshTokenToPersist = existing.getRefreshToken();
        }

        GoogleDriveCredential entity = existing != null ? existing : new GoogleDriveCredential();
        entity.setCredentialKey(key);
        entity.setAccessToken(credential.getAccessToken());
        entity.setRefreshToken(refreshTokenToPersist);
        entity.setExpirationTimeMillis(credential.getExpirationTimeMilliseconds());

        try {

            repository.save(entity);

        } catch (DataIntegrityViolationException raceOnUniqueKey) {

            // Another instance inserted the same key concurrently between
            // our findByCredentialKey() and save(). Fall back to updating
            // whatever row now exists rather than failing the request —
            // avoids a duplicate row and avoids surfacing a transient race
            // as a hard error to the caller.
            log.warn(
                    "[DRIVE_AUTH_STORE] Concurrent credential write detected for key={}, retrying as update.",
                    key
            );

            GoogleDriveCredential afterRace = repository.findByCredentialKey(key)
                    .orElseThrow(() -> raceOnUniqueKey);

            String preservedRefreshToken = credential.getRefreshToken();
            if (preservedRefreshToken == null) {
                preservedRefreshToken = afterRace.getRefreshToken();
            }

            afterRace.setAccessToken(credential.getAccessToken());
            afterRace.setRefreshToken(preservedRefreshToken);
            afterRace.setExpirationTimeMillis(credential.getExpirationTimeMilliseconds());
            repository.save(afterRace);
        }

        log.info("[DRIVE_AUTH_STORE] Credential row persisted. key={}", key);

        return this;
    }

    @Override
    @Transactional
    public DataStore<StoredCredential> clear() {
        log.warn("[DRIVE_AUTH_STORE] Clearing all credential rows.");
        repository.deleteAll();
        return this;
    }

    @Override
    @Transactional
    public DataStore<StoredCredential> delete(String key) {
        log.info("[DRIVE_AUTH_STORE] Deleting credential row. key={}", key);
        repository.deleteByCredentialKey(key);
        return this;
    }

    private StoredCredential toStoredCredential(GoogleDriveCredential row) {
        StoredCredential credential = new StoredCredential();
        credential.setAccessToken(row.getAccessToken());
        credential.setRefreshToken(row.getRefreshToken());
        credential.setExpirationTimeMilliseconds(row.getExpirationTimeMillis());
        return credential;
    }

    private boolean matches(GoogleDriveCredential row, StoredCredential value) {
        return java.util.Objects.equals(row.getAccessToken(), value.getAccessToken())
                && java.util.Objects.equals(row.getRefreshToken(), value.getRefreshToken())
                && java.util.Objects.equals(row.getExpirationTimeMillis(), value.getExpirationTimeMilliseconds());
    }
}
