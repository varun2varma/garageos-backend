package com.garageos.modules.identity.service.impl;

import com.garageos.modules.identity.service.PasswordResetNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Placeholder delivery — logs that a reset was requested, WITHOUT the
 * token itself (a logged reset token would be a real credential leak into
 * log storage, same class of mistake this codebase already avoids for
 * bearer tokens - see ApiClient's own logging and this repo's CLAUDE.md
 * §19/14). This is a genuinely incomplete delivery path, not a disguised
 * one: forgot-password cannot be exercised end-to-end (an actual user
 * cannot receive their token) until this class is replaced with a real
 * sender.
 *
 * To complete this (the ONE piece of Mission backlog #13 that needs a
 * real external credential this environment does not have):
 * 1. Add an SMTP or transactional-email dependency (e.g.
 *    spring-boot-starter-mail, or an SMS gateway SDK) - a NEW pubspec/pom
 *    dependency, so per this repo's own CLAUDE.md needs explicit
 *    approval before adding.
 * 2. Supply real credentials via environment variables, never committed:
 *    SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD, SMTP_FROM_ADDRESS
 *    (or the equivalent for an SMS gateway).
 * 3. Replace this class's body with a real send call. Nothing else in
 *    AuthServiceImpl/AuthController needs to change - this is the one
 *    seam that was deliberately built for exactly that swap.
 */
@Slf4j
@Service
public class LoggingPasswordResetNotificationService implements PasswordResetNotificationService {

    @Override
    public void sendResetToken(String destinationEmailOrMobile, String plaintextToken, String recipientFirstName) {

        log.warn(
                "Password reset requested for {} - NO REAL DELIVERY CONFIGURED "
                        + "(see LoggingPasswordResetNotificationService's own doc comment). "
                        + "The reset token was generated and stored but not sent anywhere.",
                destinationEmailOrMobile
        );
    }
}
