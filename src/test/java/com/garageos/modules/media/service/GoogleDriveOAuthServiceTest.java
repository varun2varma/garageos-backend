package com.garageos.modules.media.service;

import com.garageos.core.config.GoogleDriveProperties;
import com.garageos.modules.media.repository.GoogleDriveCredentialRepository;
import com.garageos.modules.media.service.impl.GoogleDriveCredentialDataStore;
import com.google.api.client.auth.oauth2.Credential;
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
 * the new database-backed store. Uses fake, non-secret property values
 * (never real client credentials) — none of this touches Google's network;
 * {@code createFlow()} only builds local objects, and
 * {@code flow.loadCredential(...)} only reads from the injected DataStore.
 *
 * NOT covered here: {@link GoogleDriveOAuthService#exchangeCode} itself,
 * since that method makes a real outbound HTTP call to Google's token
 * endpoint (`flow.newTokenRequest(code).execute()`) — faking that would
 * require a mock HTTP transport layer, which is out of proportion for this
 * focused feature. The persistence half of what exchangeCode does (storing
 * the resulting credential) is covered directly in
 * GoogleAuthorizationCodeFlowCredentialPersistenceTest instead.
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
        oauthService = new GoogleDriveOAuthService(properties, dataStore);
    }

    @Test
    void getStoredCredential_noRowPersisted_returnsNull() throws Exception {
        when(repository.findByCredentialKey("garagest-drive")).thenReturn(Optional.empty());

        Credential credential = oauthService.getStoredCredential();

        assertThat(credential).isNull();
    }
}
