package com.garageos.modules.identity.service.impl;

import com.garageos.modules.identity.service.PlatformAdminProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Runs the one-time platform administrator grant at startup, and only when an operator has set
 * {@code garagest.bootstrap.platform-admin.enabled=true}. The bean does not exist otherwise, so
 * by default there is no code path that can grant the role. A failed condition is logged and
 * nothing changes; the application still starts.
 *
 * Required while enabled: {@code .username}, {@code .confirm-username}, {@code .environment}.
 * Remove the configuration after the grant is logged.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "garagest.bootstrap.platform-admin.enabled", havingValue = "true")
public class PlatformAdminBootstrapRunner implements ApplicationRunner {

    private final PlatformAdminProvisioningService provisioning;

    @Value("${garagest.bootstrap.platform-admin.username:}")
    private String username;

    @Value("${garagest.bootstrap.platform-admin.confirm-username:}")
    private String confirmUsername;

    @Value("${garagest.bootstrap.platform-admin.environment:}")
    private String environment;

    @Override
    public void run(ApplicationArguments args) {
        try {
            PlatformAdminProvisioningService.Outcome outcome =
                    provisioning.provision(username, confirmUsername, environment);

            if (outcome == PlatformAdminProvisioningService.Outcome.ALREADY_PROVISIONED) {
                log.warn("[PLATFORM-ADMIN] A platform administrator already exists; nothing was changed. "
                        + "Disable the bootstrap configuration.");
            }
        } catch (RuntimeException ex) {
            log.error("[PLATFORM-ADMIN] Bootstrap refused, nothing was changed: {}", ex.getMessage());
        }
    }
}
