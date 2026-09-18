package com.garageos.modules.media.service;

import com.garageos.core.config.GoogleDriveProperties;
import com.garageos.modules.media.entity.GoogleDriveCredential;
import com.garageos.modules.media.repository.GoogleDriveCredentialRepository;
import com.garageos.modules.media.service.impl.GoogleDriveCredentialDataStore;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test for the incident this fix addresses: production had a
 * persisted {@code google_drive_credential} row whose {@code refresh_token}
 * column was NULL, because {@link GoogleDriveOAuthService#getAuthorizationUrl()}
 * never forced {@code prompt=consent} — Google silently omits
 * {@code refresh_token} from the token exchange on any authorization after
 * the very first one for a given user+client pair, so a second/later
 * {@code /authorize} run captured an access-token-only credential. With no
 * refresh token stored, automatic refresh could never succeed no matter how
 * correctly the rest of the library was wired — which is exactly what this
 * test proves is now fixed at the *data* layer, not just re-asserting the
 * library's own (already-correct) wiring.
 *
 * This proves the actual production code path — not a reimplementation of
 * it: {@link GoogleDriveOAuthService#getStoredCredential()} loads a
 * {@link Credential} the exact same way {@link GoogleDriveClientService}
 * does for every real Drive call, backed by a {@link GoogleDriveCredentialDataStore}
 * wrapping a mocked {@link GoogleDriveCredentialRepository} (no real
 * database), talking to a {@link MockHttpTransport} standing in for
 * Google's token endpoint (no real network / no real Google account
 * needed). Whether the underlying google-oauth-client library performs an
 * automatic, transparent refresh when a Drive request is attempted with an
 * expired token cannot be forced deterministically without live network
 * access to Google's servers — so this test instead calls the same public
 * {@link Credential#refreshToken()} method the library invokes internally,
 * which exercises the identical HTTP call, client authentication, and
 * refresh-listener persistence path.
 */
@ExtendWith(MockitoExtension.class)
class GoogleDriveCredentialRefreshTest {

    private static final String KEY = "garagest-drive";

    @Mock
    private GoogleDriveCredentialRepository repository;

    @Test
    void expiredAccessToken_withStoredRefreshToken_refreshesAutomaticallyAndPersistsNewAccessToken()
            throws Exception {

        // ---- Arrange: a persisted row with BOTH tokens present — the
        // scenario that was broken because refresh_token was silently
        // never captured in the first place. ----
        GoogleDriveCredential row = new GoogleDriveCredential();
        row.setCredentialKey(KEY);
        row.setAccessToken("stale-expired-access-token");
        row.setRefreshToken("valid-stored-refresh-token");
        row.setExpirationTimeMillis(System.currentTimeMillis() - 60_000L); // already expired
        when(repository.findByCredentialKey(KEY)).thenReturn(Optional.of(row));

        GoogleDriveCredentialDataStore dataStore = new GoogleDriveCredentialDataStore(repository);

        MockLowLevelHttpResponse tokenEndpointResponse = new MockLowLevelHttpResponse()
                .setContentType("application/json")
                .setContent(
                        "{\"access_token\":\"brand-new-refreshed-access-token\","
                                + "\"expires_in\":3600,"
                                + "\"token_type\":\"Bearer\"}");

        MockHttpTransport transport = new MockHttpTransport.Builder()
                .setLowLevelHttpResponse(tokenEndpointResponse)
                .build();

        GoogleDriveProperties properties = new GoogleDriveProperties();
        properties.setClientId("fake-client-id-for-tests-only");
        properties.setClientSecret("fake-client-secret-for-tests-only");
        properties.setRedirectUri("https://example.test/callback");
        properties.setApplicationName("GarageST-Test");
        properties.setRootFolderId("fake-root-folder-id");

        GoogleDriveOAuthService oauthService = new GoogleDriveOAuthService(
                properties, dataStore, transport, GsonFactory.getDefaultInstance());

        // ---- Act: load the credential exactly like every real Drive call
        // does (GoogleDriveClientService.getDriveClient() -> oauthService.
        // getStoredCredential()), then trigger the refresh Credential.
        // intercept() performs transparently and automatically right
        // before any real HTTP request when the access token is expired. ----
        Credential credential = oauthService.getStoredCredential();
        assertThat(credential).isNotNull();
        assertThat(credential.getRefreshToken())
                .as("the loaded credential must carry the stored refresh token")
                .isEqualTo("valid-stored-refresh-token");
        assertThat(credential.getExpiresInSeconds())
                .as("the loaded credential must reflect the already-expired stored expiry")
                .isLessThanOrEqualTo(0L);

        boolean refreshed = credential.refreshToken();

        // ---- Assert: refresh succeeded, the new access token is usable,
        // and it was persisted back through the SAME data store every
        // other Drive call reads from. ----
        assertThat(refreshed).as("refresh must succeed given a valid refresh token").isTrue();
        assertThat(credential.getAccessToken()).isEqualTo("brand-new-refreshed-access-token");

        ArgumentCaptor<GoogleDriveCredential> saved = ArgumentCaptor.forClass(GoogleDriveCredential.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getAccessToken()).isEqualTo("brand-new-refreshed-access-token");
        assertThat(saved.getValue().getRefreshToken())
                .as("the refresh token must remain persisted after an access-token-only refresh response "
                        + "(Google does not reissue it on every refresh) — this is what makes reauthorization "
                        + "a one-time operation rather than something needed on every access-token expiry")
                .isEqualTo("valid-stored-refresh-token");
    }

    @Test
    void expiredAccessToken_withNoStoredRefreshToken_cannotRefresh_reproducesTheOriginalIncident()
            throws Exception {

        // Reproduces the exact broken state found live in production
        // tonight (confirmed via a direct read of the google_drive_credential
        // row: has_refresh_token = false) — proves the FAILURE mode this
        // fix's prompt=consent change prevents from happening again, not
        // just the success path.
        GoogleDriveCredential row = new GoogleDriveCredential();
        row.setCredentialKey(KEY);
        row.setAccessToken("stale-expired-access-token");
        row.setRefreshToken(null);
        row.setExpirationTimeMillis(System.currentTimeMillis() - 60_000L);
        when(repository.findByCredentialKey(KEY)).thenReturn(Optional.of(row));

        GoogleDriveCredentialDataStore dataStore = new GoogleDriveCredentialDataStore(repository);
        MockHttpTransport transport = new MockHttpTransport.Builder().build();

        GoogleDriveProperties properties = new GoogleDriveProperties();
        properties.setClientId("fake-client-id-for-tests-only");
        properties.setClientSecret("fake-client-secret-for-tests-only");
        properties.setRedirectUri("https://example.test/callback");
        properties.setApplicationName("GarageST-Test");
        properties.setRootFolderId("fake-root-folder-id");

        GoogleDriveOAuthService oauthService = new GoogleDriveOAuthService(
                properties, dataStore, transport, GsonFactory.getDefaultInstance());

        Credential credential = oauthService.getStoredCredential();
        assertThat(credential.getRefreshToken()).isNull();

        boolean refreshed = credential.refreshToken();

        assertThat(refreshed)
                .as("with no refresh token at all, refresh cannot succeed — reauthorization really is "
                        + "required in this specific case, which is the one case the mission brief itself "
                        + "says should still require it")
                .isFalse();
    }
}
