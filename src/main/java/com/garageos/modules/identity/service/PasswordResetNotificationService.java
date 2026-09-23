package com.garageos.modules.identity.service;

/**
 * Delivers a password-reset token to the account holder. Mission backlog
 * #13 — the real, working piece is token generation/hashing/expiry/
 * single-use validation (AuthServiceImpl.forgotPassword/resetPassword);
 * THIS interface is the one piece that genuinely requires an external
 * credential this environment does not have (an SMTP relay or SMS
 * gateway) - see LoggingPasswordResetNotificationService's own doc
 * comment for exactly what's needed to complete it.
 */
public interface PasswordResetNotificationService {

    void sendResetToken(String destinationEmailOrMobile, String plaintextToken, String recipientFirstName);
}
