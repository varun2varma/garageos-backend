package com.garageos.modules.otp.service;

import com.garageos.core.enums.otp.OtpPurpose;

/**
 * Purpose-bound one-time passwords. Flows (registration, garage registration,
 * later phone change / password reset / login / payment verification) depend
 * only on this interface; delivery is delegated to an
 * {@link com.garageos.modules.otp.provider.OtpProvider}.
 */
public interface OtpService {

    /** Whether OTP verification is switched on (otp.enabled). When off, flows behave as before OTP existed. */
    boolean isEnabled();

    /**
     * Issues and delivers an OTP for {@code purpose} to {@code destination}
     * (a normalized 10-digit Indian mobile number for the SMS channel),
     * superseding any older live OTP for the same destination and purpose.
     *
     * @param userId the authenticated user this OTP is for, or null for pre-account flows
     */
    void send(OtpPurpose purpose, String destination, Long userId);

    /**
     * Issues and delivers an OTP to an e-mail address, under exactly the same rules as {@link #send}. Fails with
     * OTP_NOT_CONFIGURED unless an e-mail provider is configured; e-mail is never assumed available.
     */
    void sendEmail(OtpPurpose purpose, String email, Long userId);

    /**
     * Verifies and consumes the OTP in its own transaction, so a wrong-attempt
     * count survives the caller's rollback. Succeeds at most once per issued OTP.
     *
     * @throws com.garageos.modules.otp.exception.OtpException on any failure
     */
    void verifyAndConsume(OtpPurpose purpose, String destination, String otp);
}
