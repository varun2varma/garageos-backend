package com.garageos.modules.otp.provider;

import com.garageos.core.enums.otp.OtpChannel;
import com.garageos.core.enums.otp.OtpPurpose;

/**
 * Delivers an OTP over one channel. OtpService owns generation, hashing,
 * expiry and rate limiting; a provider only transports the message, so a
 * WhatsApp or e-mail provider can be added without touching any flow.
 */
public interface OtpProvider {

    OtpChannel channel();

    /** Short stable name stored on the challenge (e.g. "MESSAGE_CENTRAL"). */
    String name();

    /** True when every credential this provider needs is configured. */
    boolean isConfigured();

    /** Outcome of asking a self-validating provider whether a code is correct. */
    enum CodeCheck { VALID, WRONG, EXPIRED, ALREADY_VERIFIED }

    /**
     * True when the provider generates the OTP itself and validates it
     * server-side (Message Central VerifyNow). OtpService then does not
     * generate or hash a code: {@link #send} returns the provider's
     * verification reference and {@link #validate} decides whether a typed
     * code is right. Every other rule (expiry, attempts, cooldown, rate
     * limits, error codes) stays in OtpService.
     */
    default boolean validatesCodeItself() {
        return false;
    }

    /**
     * Only called when {@link #validatesCodeItself()} is true.
     *
     * @param reference the value {@link #send} returned for this challenge
     * @throws com.garageos.modules.otp.exception.OtpException OTP_PROVIDER_FAILURE when the answer cannot be obtained or classified
     */
    default CodeCheck validate(String reference, String destination, String code) {
        throw new UnsupportedOperationException("This provider does not validate codes itself.");
    }

    /**
     * Sends {@code otp} to {@code destination}. {@code otp} is {@code null}
     * for a provider that {@linkplain #validatesCodeItself() generates its own code}.
     *
     * @return the provider's message/verification reference, or null when it gives none
     * @throws com.garageos.modules.otp.exception.OtpException OTP_PROVIDER_FAILURE on any delivery failure
     */
    String send(String destination, String otp, OtpPurpose purpose);
}
