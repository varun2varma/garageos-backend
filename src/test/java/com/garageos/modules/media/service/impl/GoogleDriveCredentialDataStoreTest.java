package com.garageos.modules.media.service.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.garageos.modules.media.entity.GoogleDriveCredential;
import com.garageos.modules.media.repository.GoogleDriveCredentialRepository;
import com.google.api.client.auth.oauth2.StoredCredential;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the PostgreSQL-backed {@link StoredCredential} store that
 * replaced the local-disk FileDataStoreFactory. No Spring context / real
 * database — {@link GoogleDriveCredentialRepository} is mocked throughout,
 * matching this repository's existing no-DB-test-infrastructure pattern
 * (see MediaServiceImplTest).
 */
@ExtendWith(MockitoExtension.class)
class GoogleDriveCredentialDataStoreTest {

    private static final String KEY = "garagest-drive";

    @Mock
    private GoogleDriveCredentialRepository repository;

    private GoogleDriveCredentialDataStore dataStore;

    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        dataStore = new GoogleDriveCredentialDataStore(repository);

        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(GoogleDriveCredentialDataStore.class)).addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(GoogleDriveCredentialDataStore.class)).detachAppender(logAppender);
    }

    // ------------------------------------------------------------
    // 1. No stored credential -> get() returns null
    // ------------------------------------------------------------
    @Test
    void get_noStoredRow_returnsNull() throws Exception {
        when(repository.findByCredentialKey(KEY)).thenReturn(Optional.empty());

        assertThat(dataStore.get(KEY)).isNull();
    }

    // ------------------------------------------------------------
    // 2. Stored credential -> get() loads it, including the refresh
    //    token (required for automatic access-token refresh).
    // ------------------------------------------------------------
    @Test
    void get_storedRow_loadsAccessAndRefreshTokenAndExpiry() throws Exception {
        GoogleDriveCredential row = new GoogleDriveCredential();
        row.setCredentialKey(KEY);
        row.setAccessToken("fake-access-token");
        row.setRefreshToken("fake-refresh-token");
        row.setExpirationTimeMillis(123456789L);
        when(repository.findByCredentialKey(KEY)).thenReturn(Optional.of(row));

        StoredCredential loaded = dataStore.get(KEY);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getAccessToken()).isEqualTo("fake-access-token");
        assertThat(loaded.getRefreshToken()).isEqualTo("fake-refresh-token");
        assertThat(loaded.getExpirationTimeMilliseconds()).isEqualTo(123456789L);
    }

    // ------------------------------------------------------------
    // 3 / 5. set() with no existing row inserts one; set() again
    //    (simulating a refreshed access token) updates the SAME row
    //    rather than creating a second one.
    // ------------------------------------------------------------
    @Test
    void set_noExistingRow_insertsNewRow() throws Exception {
        when(repository.findByCredentialKey(KEY)).thenReturn(Optional.empty());

        StoredCredential credential = new StoredCredential();
        credential.setAccessToken("access-1");
        credential.setRefreshToken("refresh-1");
        credential.setExpirationTimeMilliseconds(1000L);

        dataStore.set(KEY, credential);

        ArgumentCaptor<GoogleDriveCredential> saved = ArgumentCaptor.forClass(GoogleDriveCredential.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getCredentialKey()).isEqualTo(KEY);
        assertThat(saved.getValue().getAccessToken()).isEqualTo("access-1");
        assertThat(saved.getValue().getRefreshToken()).isEqualTo("refresh-1");
    }

    @Test
    void set_existingRow_updatesInPlace_doesNotCreateDuplicate() throws Exception {
        GoogleDriveCredential existing = new GoogleDriveCredential();
        existing.setCredentialKey(KEY);
        existing.setAccessToken("old-access");
        existing.setRefreshToken("old-refresh");
        when(repository.findByCredentialKey(KEY)).thenReturn(Optional.of(existing));

        StoredCredential refreshed = new StoredCredential();
        refreshed.setAccessToken("new-access");
        refreshed.setRefreshToken("new-refresh");
        refreshed.setExpirationTimeMilliseconds(2000L);

        dataStore.set(KEY, refreshed);

        ArgumentCaptor<GoogleDriveCredential> saved = ArgumentCaptor.forClass(GoogleDriveCredential.class);
        verify(repository, times(1)).save(saved.capture());
        assertThat(saved.getValue()).isSameAs(existing);
        assertThat(saved.getValue().getAccessToken()).isEqualTo("new-access");
    }

    // ------------------------------------------------------------
    // 6. Refresh response without a new refresh token must not blank
    //    out the previously stored one.
    // ------------------------------------------------------------
    @Test
    void set_refreshResponseWithoutRefreshToken_preservesPreviouslyStoredRefreshToken() throws Exception {
        GoogleDriveCredential existing = new GoogleDriveCredential();
        existing.setCredentialKey(KEY);
        existing.setAccessToken("old-access");
        existing.setRefreshToken("still-valid-refresh-token");
        when(repository.findByCredentialKey(KEY)).thenReturn(Optional.of(existing));

        // Simulates what DataStoreCredentialRefreshListener passes after a
        // plain access-token refresh: Google did not return a new refresh
        // token, so the incoming StoredCredential has a null one.
        StoredCredential refreshedAccessTokenOnly = new StoredCredential();
        refreshedAccessTokenOnly.setAccessToken("brand-new-access-token");
        refreshedAccessTokenOnly.setRefreshToken(null);
        refreshedAccessTokenOnly.setExpirationTimeMilliseconds(9999L);

        dataStore.set(KEY, refreshedAccessTokenOnly);

        ArgumentCaptor<GoogleDriveCredential> saved = ArgumentCaptor.forClass(GoogleDriveCredential.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getAccessToken()).isEqualTo("brand-new-access-token");
        assertThat(saved.getValue().getRefreshToken())
                .as("a missing refresh token in the refresh response must not erase the previously stored one")
                .isEqualTo("still-valid-refresh-token");
    }

    // ------------------------------------------------------------
    // 7. Duplicate key prevented even under a concurrent-insert race
    //    (two instances both see "no existing row" and both try to
    //    insert): the unique-constraint violation is caught and
    //    retried as an update against whichever row now exists.
    // ------------------------------------------------------------
    @Test
    void set_concurrentInsertRace_fallsBackToUpdateInsteadOfDuplicating() throws Exception {
        GoogleDriveCredential rowInsertedByOtherInstance = new GoogleDriveCredential();
        rowInsertedByOtherInstance.setCredentialKey(KEY);
        rowInsertedByOtherInstance.setAccessToken("access-from-other-instance");
        rowInsertedByOtherInstance.setRefreshToken("refresh-from-other-instance");

        // First lookup (before our own save attempt): nothing there yet.
        // Second lookup (after our save() throws a unique-constraint
        // violation, i.e. the other instance won the race): the row it
        // inserted is now visible.
        when(repository.findByCredentialKey(KEY))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(rowInsertedByOtherInstance));

        when(repository.save(any(GoogleDriveCredential.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StoredCredential ourCredential = new StoredCredential();
        ourCredential.setAccessToken("our-access");
        ourCredential.setRefreshToken("our-refresh");

        dataStore.set(KEY, ourCredential);

        verify(repository, times(2)).save(any(GoogleDriveCredential.class));
        // Exactly one logical row is ever touched for this key — the retry
        // updates rowInsertedByOtherInstance rather than creating another.
    }

    // ------------------------------------------------------------
    // 8. No credential values are ever written to logs.
    // ------------------------------------------------------------
    @Test
    void set_neverLogsTokenValues() throws Exception {
        when(repository.findByCredentialKey(KEY)).thenReturn(Optional.empty());

        StoredCredential credential = new StoredCredential();
        credential.setAccessToken("SECRET-ACCESS-TOKEN-VALUE");
        credential.setRefreshToken("SECRET-REFRESH-TOKEN-VALUE");
        credential.setExpirationTimeMilliseconds(1L);

        dataStore.set(KEY, credential);
        dataStore.get(KEY);

        String allLogMessages = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + " " + b);

        assertThat(allLogMessages).doesNotContain("SECRET-ACCESS-TOKEN-VALUE");
        assertThat(allLogMessages).doesNotContain("SECRET-REFRESH-TOKEN-VALUE");
    }
}
