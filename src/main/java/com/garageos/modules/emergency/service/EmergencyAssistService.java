package com.garageos.modules.emergency.service;

import com.garageos.modules.emergency.dto.request.EmergencyRequests;
import com.garageos.modules.emergency.dto.response.EmergencyResponses;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;

import java.util.List;

/**
 * Emergency Assist: a pre-login, OTP-verified roadside request routed to a server-computed eligible garage.
 * The public half is addressed by a verification token (proof of the number) and a status token (proof of the
 * request); the garage half is authenticated and garage-scoped.
 */
public interface EmergencyAssistService {

    EmergencyResponses.VerificationSent sendVerification(String mobile);

    EmergencyResponses.VerificationConfirmed confirmVerification(String mobile, String otp);

    EmergencyResponses.GarageOptions eligibleGarages(EmergencyRequests.SearchGarages request);

    /** Creates the request, or returns the existing one when the same idempotency key is retried. */
    EmergencyResponses.RequestView create(String idempotencyKey, EmergencyRequests.Create request);

    EmergencyResponses.RequestView status(String requestCode, String statusToken);

    EmergencyResponses.RequestView cancel(String requestCode, String statusToken);

    List<EmergencyResponses.GarageRequestView> garageRequests(GarageUserPrincipal principal);

    EmergencyResponses.GarageRequestView accept(Long id, String remarks, GarageUserPrincipal principal);

    EmergencyResponses.GarageRequestView reject(Long id, String remarks, GarageUserPrincipal principal);

    EmergencyResponses.GarageRequestView complete(Long id, String remarks, GarageUserPrincipal principal);

    /** Scheduled housekeeping: unanswered requests expire, long-accepted ones close. */
    void expireStale();
}
