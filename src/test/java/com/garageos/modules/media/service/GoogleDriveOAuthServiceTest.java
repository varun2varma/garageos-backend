package com.garageos.modules.media.service;

import com.garageos.core.config.GoogleDriveProperties;
import com.garageos.modules.media.repository.GoogleDriveCredentialRepository;
import com.garageos.modules.media.service.impl.GoogleDriveCredentialDataStore;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.testing.http.MockHttpTransport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Exercises {@link GoogleDriveOAuthService#getStoredCredential()} against
 * the database-backed store. Uses fake, non-secret property values (never
 * real client credentials) and a no-op {@link MockHttpTransport} — none of
 * this touches Google's real network; {@code createFlow()} only builds
 * local objects, and {@code flow.loadCredential(...)} only reads from the
 * injected DataStore.
 *
 * NOT covered here: {@link GoogleDriveOAuthService#exchangeCode} itself,
 * since asserting its real token-exchange behavior needs a transport that
 * actually returns a token response — see {@link GoogleDriveCredentialRefreshTest}
 * for that (credential-refresh is the same underlying HTTP call shape and
 * is the behavior that actually broke in production).
 */
@ExtendWith(MockitoExtension.class)
class GoogleDriveOAuthServiceTest {

    @Mock
    private GoogleDriveCredentialRepository repository;

    private GoogleDriveOAuthService oauthService;

    @BeforeEach
    void setUp() {
        GoogleDriveProperties properties = new GoogleDriveProperties();
        properties.setClientId("fake-client-id-for-tests-only");
        properties.setClientSecret("fake-client-secret-for-tests-only");
        properties.setRedirectUri("https://example.test/callback");
        properties.setApplicationName("GarageST-Test");
        properties.setRootFolderId("fake-root-folder-id");

        GoogleDriveCredentialDataStore dataStore = new GoogleDriveCredentialDataStore(repository);
        MockHttpTransport transport = new MockHttpTransport.Builder().build();
        oauthService = new GoogleDriveOAuthService(properties, dataStore, transport, GsonFactory.getDefaultInstance());
    }

    @Test
    void getStoredCredential_noRowPersisted_returnsNull() throws Exception {
        when(repository.findByCredentialKey("garagest-drive")).thenReturn(Optional.empty());

        Credential credential = oauthService.getStoredCredential();

        assertThat(credential).isNull();
    }

    // ------------------------------------------------------------
    // Regression: the root cause of the production incident.
    // access_type=offline alone does not guarantee Google returns a
    // refresh_token on every /authorize — only on the first-ever consent
    // for a given user+client pair. Without prompt=consent, a second
    // authorization (e.g. after this exact bug, or after any
    // already-granted-access scenario) silently captures an
    // access-token-only credential that can never self-refresh.
    // ------------------------------------------------------------
    @Test
    void getAuthorizationUrl_forcesConsentPrompt_soRefreshTokenIsAlwaysReissued() throws Exception {
        String authorizationUrl = oauthService.getAuthorizationUrl();

        assertThat(authorizationUrl)
                .as("without prompt=consent, Google silently omits refresh_token from the token "
                        + "exchange for any authorization after the very first one — this is the exact "
                        + "root cause of the production incident this fix addresses")
                .contains("prompt=consent");
        assertThat(authorizationUrl).contains("access_type=offline");
    }
}
