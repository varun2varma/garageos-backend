package com.garageos.modules.identity.service;

import com.garageos.modules.identity.security.principal.GarageUserPrincipal;

/**
 * Proves that the signed-in user controls the e-mail address on their own account. The address is always read from
 * the stored account, never from the request, so nobody can use this to verify (or probe) someone else's mailbox.
 * Verification proves control of the mailbox only; it says nothing about bank ownership or identity.
 */
public interface EmailVerificationService {

    /** Sends a one-time code to the caller's own stored e-mail address. */
    void sendOwn(GarageUserPrincipal principal);

    /** Verifies the code and records the address as verified. */
    void verifyOwn(GarageUserPrincipal principal, String otp);
}
