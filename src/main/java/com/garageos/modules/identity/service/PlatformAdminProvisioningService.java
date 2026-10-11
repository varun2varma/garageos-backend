package com.garageos.modules.identity.service;

/**
 * Operator-controlled, one-time grant of the platform administrator (SUPER_ADMIN) role to an
 * account that is ALREADY registered. There is deliberately no API for this: it is reachable only
 * from the startup bootstrap, which is disabled unless an operator sets explicit configuration.
 *
 * The grant never creates an account, never handles a password, and refuses unless every safety
 * condition holds (see the implementation). It is a no-op once any platform administrator exists.
 */
public interface PlatformAdminProvisioningService {

    enum Outcome {
        /** The role was granted by this call. */
        GRANTED,
        /** A platform administrator already exists; nothing was changed. */
        ALREADY_PROVISIONED
    }

    /**
     * @param username        the registered account that should become the platform administrator
     * @param confirmUsername must equal {@code username} (guards against a typo in the configuration)
     * @param environment     the operator-declared environment; must be one of the active Spring profiles
     * @throws com.garageos.core.exception.BusinessException when any condition is not met; nothing is changed
     */
    Outcome provision(String username, String confirmUsername, String environment);
}
