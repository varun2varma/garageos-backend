package com.garageos.modules.otp.service;

import com.garageos.core.enums.otp.OtpPurpose;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.otp.dto.response.SendOtpResponse;

/**
 * Resolves WHO an OTP goes to for each supported purpose, enforcing that
 * server-side, then delegates to {@link OtpService}. The destination is never
 * trusted from the client except for USER_REGISTRATION, where no account
 * exists yet.
 */
public interface OtpFlowService {

    SendOtpResponse send(OtpPurpose purpose, String mobile, GarageUserPrincipal principalOrNull);
}
