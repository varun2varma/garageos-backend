package com.garageos.modules.identity.service;

import com.garageos.modules.identity.dto.request.ChangePasswordRequest;
import com.garageos.modules.identity.dto.request.LoginRequest;
import com.garageos.modules.identity.dto.request.RegisterRequest;
import com.garageos.modules.identity.dto.response.LoginResponse;
import com.garageos.modules.identity.dto.response.RegisterResponse;
import com.garageos.modules.identity.dto.response.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    LoginResponse login(
            LoginRequest request,
            HttpServletRequest servletRequest);

    void logout(HttpServletRequest request);

    void changePassword(ChangePasswordRequest request);

    LoginResponse refreshToken(String refreshToken);

    UserProfileResponse me();

    /** Mission backlog #14 — self-service profile edit, for the calling user only. */
    UserProfileResponse updateProfile(com.garageos.modules.identity.dto.request.UpdateProfileRequest request);

    /**
     * Mission backlog #13 — forgot password, step 1. Always returns
     * normally (never reveals whether the identifier matched an account -
     * standard practice against account enumeration), even though actual
     * delivery is not yet configured (see LoggingPasswordResetNotificationService).
     */
    void forgotPassword(com.garageos.modules.identity.dto.request.ForgotPasswordRequest request);

    /** Mission backlog #13 — forgot password, step 2: consume the token and set a new password. */
    void resetPassword(com.garageos.modules.identity.dto.request.ResetPasswordRequest request);

    RegisterResponse register(RegisterRequest request);

    /**
     * Soft-deletes the calling user's own account: marks it DELETED (which
     * GarageUserPrincipal already treats as disabled/locked, blocking both
     * fresh login and any still-valid JWT on re-check), revokes all
     * sessions, and clears/anonymizes directly-identifying fields. Business
     * records owned by other aggregates (customers, vehicles, job cards,
     * invoices, etc.) are untouched. Idempotent: calling this again on an
     * already-deleted account is a no-op.
     */
    void deleteAccount();

    /**
     * Google Play external account-deletion requirement: called from the
     * public, unauthenticated /delete-account web form. Accepts a
     * self-reported email/mobile/username identifier from an anonymous
     * visitor and, if it matches a real account, records a deletion
     * request. Deliberately generic in outcome either way (see this
     * method's implementation) to avoid account enumeration, and never
     * itself deletes anything - see AuthServiceImpl's own doc comment for
     * why the actual destructive step is a separate, not-yet-built path.
     */
    void requestAccountDeletion(String identifier, String clientIp);

}