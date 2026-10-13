package com.garageos.modules.identity.service.impl;

import com.garageos.core.enums.identity.AccountDeletionRequestStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.identity.dto.request.ChangePasswordRequest;
import com.garageos.modules.identity.dto.request.ForgotPasswordRequest;
import com.garageos.modules.identity.dto.request.LoginRequest;
import com.garageos.core.enums.otp.OtpPurpose;
import com.garageos.modules.identity.dto.request.VerifyResetOtpRequest;
import com.garageos.modules.identity.dto.response.PasswordResetAuthorizationResponse;
import com.garageos.modules.otp.exception.OtpException;
import com.garageos.modules.otp.service.OtpService;
import com.garageos.modules.identity.dto.request.RegisterRequest;
import com.garageos.modules.identity.dto.request.ResetPasswordRequest;
import com.garageos.modules.identity.dto.response.LoginResponse;
import com.garageos.modules.identity.dto.response.RegisterResponse;
import com.garageos.modules.identity.dto.response.UserProfileResponse;
import com.garageos.modules.identity.entity.AccountDeletionRequest;
import com.garageos.modules.identity.entity.PasswordResetToken;
import com.garageos.modules.identity.entity.Role;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.entity.UserRole;
import com.garageos.modules.identity.entity.UserSession;
import com.garageos.modules.identity.repository.AccountDeletionRequestRepository;
import com.garageos.modules.identity.repository.PasswordResetTokenRepository;
import com.garageos.modules.identity.repository.RoleRepository;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.repository.UserSessionRepository;
import com.garageos.modules.identity.security.jwt.JwtService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.identity.security.service.GarageUserDetailsService;
import com.garageos.modules.identity.service.AuthService;
import com.garageos.modules.identity.service.PasswordResetNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int RESET_TOKEN_LENGTH_BYTES = 24;
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 30;
    /** Reset authorization issued after a verified OTP: short-lived and single-use. */
    private static final int OTP_RESET_AUTHORIZATION_MINUTES = 10;

    // Minimal in-memory throttle for the public, unauthenticated
    // account-deletion-request form - not a general-purpose rate-limiting
    // framework (none exists in this codebase; see this class's own
    // requestAccountDeletion doc comment), just enough to stop a single
    // caller from hammering the identifier-lookup query. Per-process only;
    // resets on restart and isn't shared across instances, which is
    // acceptable for this small, non-critical protection.
    private static final int DELETION_REQUEST_MAX_PER_WINDOW = 5;
    private static final Duration DELETION_REQUEST_WINDOW = Duration.ofMinutes(15);
    private final ConcurrentHashMap<String, Deque<LocalDateTime>> deletionRequestAttemptsByIp =
            new ConcurrentHashMap<>();

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final UserRepository userRepository;

    private final OtpService otpService;

    private final UserSessionRepository userSessionRepository;

    private final GarageUserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final PasswordResetNotificationService passwordResetNotificationService;

    private final AccountDeletionRequestRepository accountDeletionRequestRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public LoginResponse login(
            LoginRequest request,
            HttpServletRequest servletRequest) {

        Authentication authentication =
                authenticationManager.authenticate(

                        new UsernamePasswordAuthenticationToken(

                                request.getUsername(),

                                request.getPassword()
                        ));

        GarageUserPrincipal principal =
                (GarageUserPrincipal) authentication.getPrincipal();

        String accessToken =
                jwtService.generateAccessToken(principal);

        String refreshToken =
                jwtService.generateRefreshToken(principal);

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() ->
                        new IllegalStateException("User not found"));

        UserSession session =
                UserSession.builder()
                        .user(user)
                        .refreshToken(refreshToken)
                        .ipAddress(servletRequest.getRemoteAddr())
                        .userAgent(servletRequest.getHeader("User-Agent"))
                        .expiresAt(
                                LocalDateTime.now()
                                        .plus(Duration.ofMillis(jwtService.getRefreshTokenExpiration().toMillis()))
                        )
                        .revoked(false)
                        .build();

        userSessionRepository.save(session);

        UserProfileResponse profile =
                buildUserProfile(principal);

        return LoginResponse.builder()

                .accessToken(accessToken)

                .refreshToken(refreshToken)

                .expiresIn(jwtService.getAccessTokenExpiration().toSeconds())

                .firstLogin(principal.getFirstLogin())

                .user(profile)

                .build();
    }

    private UserProfileResponse buildUserProfile(
            GarageUserPrincipal principal) {

        return UserProfileResponse.builder()

                .id(principal.getId())

                .garageId(principal.getGarageId())

                .username(principal.getUsername())

                .firstName(principal.getFirstName())

                .lastName(principal.getLastName())

                .email(principal.getEmail())

                .mobile(principal.getMobile())

                .mobileVerified(userRepository.findById(principal.getId()).map(User::isMobileVerified).orElse(false))

                .status(principal.getStatus())

                .firstLogin(principal.getFirstLogin())

                .roles(principal.getRoles())

                .permissions(principal.getPermissions())

                .build();
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest request) {

        String token = jwtService.extractToken(request);

        if (token == null) {
            return;
        }

        UserSession session = userSessionRepository
                .findByRefreshToken(token)
                .orElse(null);

        if (session != null) {

            session.setRevoked(Boolean.TRUE);

            userSessionRepository.save(session);
        }
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPasswordHash())) {

            throw new BusinessException("Current password is incorrect");
        }

        user.setPasswordHash(
                passwordEncoder.encode(request.getNewPassword()));

        user.setFirstLogin(false);

        userRepository.save(user);

        userSessionRepository.revokeAllByUserId(
                user.getId());
    }

    @Override
    @Transactional
    public void deleteAccount() {

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // Idempotent: a second call against an already-deleted account is a
        // no-op rather than an error - the account is already inaccessible
        // and re-anonymizing already-cleared fields would be pointless.
        if (user.getStatus() == UserStatus.DELETED) {
            return;
        }

        // username has a NOT NULL + unique DB constraint, so it can't be
        // cleared outright - anonymize it to a value that's guaranteed
        // unique (derived from the user's own id) instead. email/mobile are
        // nullable with unique constraints, so those are simply cleared,
        // freeing that email/mobile for a future registration.
        user.setUsername("deleted_user_" + user.getId());
        user.setEmail(null);
        user.setMobile(null);
        user.setFirstName("Deleted");
        user.setLastName(null);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setStatus(UserStatus.DELETED);

        userRepository.save(user);

        // Belt-and-braces alongside the status change: GarageUserPrincipal
        // already treats DELETED as disabled/locked (blocking both fresh
        // login and any still-valid access token on its next per-request
        // reload), and this also invalidates outstanding refresh tokens,
        // matching changePassword/resetPassword's own session-revocation
        // step.
        userSessionRepository.revokeAllByUserId(user.getId());
    }

    /**
     * Google Play external account-deletion requirement. This method ONLY
     * records a request row (status REQUESTED) against the matched
     * account's id - it deliberately does NOT call deleteAccount(). Two
     * reasons:
     *
     * 1. Security: the caller here is an anonymous website visitor who has
     *    typed an email/mobile into a public HTML form (no authentication
     *    at all). Knowing someone else's email or phone number must never
     *    be enough to destroy their account - the actual destructive
     *    operation stays behind DELETE /api/v1/auth/account, which can only
     *    ever act on the calling user's own authenticated session.
     * 2. There is no real verification/notification channel in this
     *    codebase yet to confirm the request came from the account owner -
     *    see LoggingPasswordResetNotificationService's own doc comment,
     *    which documents the same gap for password-reset delivery. Wiring
     *    up real verification (e.g. emailing/texting a confirmation link
     *    that, once clicked, calls deleteAccount() on this request's
     *    userId) and/or an admin review step is a deliberately separate,
     *    not-yet-built follow-on - out of scope here.
     *
     * Outcome is intentionally identical whether or not the identifier
     * matches an account, an already-deleted account, or a rate-limited
     * caller - same account-enumeration reasoning as forgotPassword.
     */
    @Override
    @Transactional
    public void requestAccountDeletion(String identifier, String clientIp) {

        if (!withinRateLimit(clientIp)) {
            return;
        }

        if (identifier == null || identifier.isBlank()) {
            return;
        }

        String trimmed = identifier.trim();

        User user = userRepository.findByUsername(trimmed)
                .or(() -> userRepository.findByEmail(trimmed))
                .or(() -> userRepository.findByMobile(trimmed))
                .orElse(null);

        if (user == null || user.getStatus() == UserStatus.DELETED) {
            log.debug("Account deletion requested for unknown/already-deleted identifier.");
            return;
        }

        boolean alreadyPending = accountDeletionRequestRepository
                .existsByUserIdAndStatus(user.getId(), AccountDeletionRequestStatus.REQUESTED);

        if (alreadyPending) {
            return;
        }

        AccountDeletionRequest deletionRequest = AccountDeletionRequest.builder()
                .userId(user.getId())
                .build();

        accountDeletionRequestRepository.save(deletionRequest);
    }

    private boolean withinRateLimit(String clientIp) {

        String key = (clientIp == null || clientIp.isBlank()) ? "unknown" : clientIp;
        LocalDateTime now = LocalDateTime.now();

        Deque<LocalDateTime> attempts =
                deletionRequestAttemptsByIp.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (attempts) {

            while (!attempts.isEmpty()
                    && attempts.peekFirst().isBefore(now.minus(DELETION_REQUEST_WINDOW))) {
                attempts.pollFirst();
            }

            if (attempts.size() >= DELETION_REQUEST_MAX_PER_WINDOW) {
                return false;
            }

            attempts.addLast(now);
            return true;
        }
    }

    /** Username, then email, then mobile - the same precedence login uses. */
    private User resolveByIdentifier(String raw) {

        if (raw == null || raw.isBlank()) {
            return null;
        }

        String identifier = raw.trim();

        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .or(() -> userRepository.findByMobile(identifier))
                .filter(u -> u.getStatus() != UserStatus.DELETED)
                .orElse(null);
    }

    /** SMS recovery may only target a number the user has proven they control (V77), never just a stored one. */
    private static boolean hasEligibleMobile(User user) {
        return user.isMobileVerified() && user.getMobile().matches("^[6-9]\\d{9}$");
    }

    // Deliberately not @Transactional: OtpService.send runs in its own transaction and may throw (cooldown, provider
    // outage); swallowing that inside an outer transaction would mark it rollback-only.
    @Override
    public void forgotPassword(ForgotPasswordRequest request) {

        User user = resolveByIdentifier(request.getIdentifier());

        // Deliberately does not throw when no account matches - revealing
        // "no such account" here is an account-enumeration vector. The
        // caller always sees the same generic outcome either way.
        if (user == null) {
            log.debug("Password reset requested for unknown identifier.");
            return;
        }

        if (otpService.isEnabled()) {
            // SMS OTP to the account's OWN registered mobile (never a client-supplied number), through the same
            // provider integration registration uses. Failures (cooldown, hourly limit, provider outage) are
            // logged and swallowed: surfacing them only for existing accounts would reveal which identifiers
            // exist. The client enforces its own resend cooldown.
            if (!hasEligibleMobile(user)) {
                log.debug("Password reset requested for an account without an eligible mobile number.");
                return;
            }
            try {
                otpService.send(OtpPurpose.PASSWORD_RESET, user.getMobile(), user.getId());
            } catch (OtpException e) {
                log.info("[PASSWORD-RESET] OTP not sent. code={}", e.getCode());
            }
            return;
        }

        // OTP switched off (otp.enabled=false): the original delivery-stub token flow, unchanged in behaviour.
        String plaintextToken = issueResetToken(user.getId(), RESET_TOKEN_EXPIRY_MINUTES);

        String destination = user.getEmail() != null && !user.getEmail().isBlank() ? user.getEmail() : user.getMobile();

        passwordResetNotificationService.sendResetToken(destination, plaintextToken, user.getFirstName());
    }

    @Override
    @Transactional(noRollbackFor = OtpException.class)
    public PasswordResetAuthorizationResponse verifyPasswordResetOtp(VerifyResetOtpRequest request) {

        if (!otpService.isEnabled()) {
            throw new OtpException(OtpException.Code.OTP_DISABLED, "OTP verification is not enabled.");
        }

        User user = resolveByIdentifier(request.getIdentifier());

        if (user == null || !hasEligibleMobile(user)) {
            // Same answer as a wrong code, so this endpoint cannot be used to probe for accounts.
            throw new OtpException(OtpException.Code.OTP_INVALID, "Invalid OTP. Please request a new one.");
        }

        // Purpose-bound and single-use: a registration OTP can never satisfy this, nor this one registration.
        otpService.verifyAndConsume(OtpPurpose.PASSWORD_RESET, user.getMobile(), request.getOtp());

        String authorization = issueResetToken(user.getId(), OTP_RESET_AUTHORIZATION_MINUTES);

        return PasswordResetAuthorizationResponse.builder()
                .resetToken(authorization)
                .expiresInSeconds(OTP_RESET_AUTHORIZATION_MINUTES * 60L)
                .build();
    }

    /** Supersedes older unused authorizations; stores only a hash. Returns {@code id.secret}. */
    private String issueResetToken(Long userId, int validForMinutes) {

        for (PasswordResetToken existing : passwordResetTokenRepository.findByUserIdAndUsedFalse(userId)) {
            existing.setUsed(true);
            passwordResetTokenRepository.save(existing);
        }

        byte[] randomBytes = new byte[RESET_TOKEN_LENGTH_BYTES];
        secureRandom.nextBytes(randomBytes);
        String secret = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        PasswordResetToken token = PasswordResetToken.builder()
                .userId(userId)
                .tokenHash(passwordEncoder.encode(secret))
                .expiresAt(LocalDateTime.now().plusMinutes(validForMinutes))
                .used(false)
                .build();

        token = passwordResetTokenRepository.saveAndFlush(token);

        return token.getId() + "." + secret;
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        String presented = request.getToken() == null ? "" : request.getToken().trim();
        int dot = presented.indexOf('.');
        Long tokenId = null;
        if (dot > 0) {
            try {
                tokenId = Long.parseLong(presented.substring(0, dot));
            } catch (NumberFormatException ignored) {
                tokenId = null;
            }
        }

        BusinessException invalid = new BusinessException(
                "This reset authorization is invalid or has expired. Please start again.");

        if (tokenId == null) {
            throw invalid;
        }

        PasswordResetToken token = passwordResetTokenRepository.findById(tokenId).orElseThrow(() -> invalid);

        if (Boolean.TRUE.equals(token.getUsed())
                || !token.getExpiresAt().isAfter(LocalDateTime.now())
                || !passwordEncoder.matches(presented.substring(dot + 1), token.getTokenHash())) {
            throw invalid;
        }

        User user = userRepository.findById(token.getUserId()).orElseThrow(() -> invalid);

        // Atomic single use: concurrent requests with the same authorization - only one gets 1 here.
        if (passwordResetTokenRepository.consumeIfValid(token.getId(), LocalDateTime.now()) != 1) {
            throw invalid;
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFirstLogin(false);
        userRepository.save(user);

        userSessionRepository.revokeAllByUserId(user.getId());
    }

    @Override
    @Transactional(noRollbackFor = OtpException.class)
    public void verifyOwnMobile(Long userId, String otp) {

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (user.getMobile() == null || !user.getMobile().matches("^[6-9]\\d{9}$")) {
            throw new BusinessException("Your account has no valid mobile number to verify.");
        }

        if (!otpService.isEnabled()) {
            throw new OtpException(OtpException.Code.OTP_DISABLED, "OTP verification is not enabled.");
        }

        // Always the authenticated user's own stored number, never client input.
        otpService.verifyAndConsume(OtpPurpose.MOBILE_VERIFICATION, user.getMobile(), otp);

        user.markMobileVerified(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(String refreshToken) {

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        UserSession session = userSessionRepository
                .findByRefreshTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid refresh token"));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token expired");
        }

        User user = session.getUser();

        GarageUserPrincipal principal =
                (GarageUserPrincipal) userDetailsService
                        .loadUserByUsername(user.getUsername());

        String accessToken =
                jwtService.generateAccessToken(principal);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenExpiration().toSeconds())
                .firstLogin(principal.getFirstLogin())
                .user(buildUserProfile(principal))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse me() {

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        return buildUserProfile(principal);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(
            com.garageos.modules.identity.dto.request.UpdateProfileRequest request) {

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new com.garageos.core.exception.ResourceNotFoundException(
                        "User not found."));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());

        user = userRepository.save(user);

        // Built from the freshly-saved entity for name/email (the JWT
        // principal is a snapshot from login/refresh time and would still
        // show the old values until the next token issue), but roles/
        // permissions/status are unchanged, so still read from principal
        // rather than re-deriving authorities here.
        return UserProfileResponse.builder()
                .id(principal.getId())
                .garageId(principal.getGarageId())
                .username(principal.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mobile(principal.getMobile())
                .mobileVerified(user.isMobileVerified())
                .status(principal.getStatus())
                .firstLogin(principal.getFirstLogin())
                .roles(principal.getRoles())
                .permissions(principal.getPermissions())
                .build();
    }


    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {

            throw new BusinessException(
                    "Username already exists.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {

            throw new BusinessException(
                    "Email already exists.");
        }

        if (userRepository.existsByMobile(request.getMobile())) {

            throw new BusinessException(
                    "Mobile number already exists.");
        }

        // After the duplicate checks so a number that cannot register never burns an OTP;
        // before any write so registration cannot complete without it.
        boolean mobileProven = false;
        if (otpService.isEnabled()) {
            otpService.verifyAndConsume(
                    OtpPurpose.USER_REGISTRATION, request.getMobile(), request.getOtp());
            mobileProven = true;
        }

        Role role =
                roleRepository.findByCode(RoleCode.USER)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Default role USER not found."));

        User user =
                User.builder()

                        .username(request.getUsername())

                        .passwordHash(
                                passwordEncoder.encode(
                                        request.getPassword()))

                        .firstName(request.getFirstName())

                        .lastName(request.getLastName())

                        .mobile(request.getMobile())

                        .email(request.getEmail())

                        .build();

        UserRole userRole =
                UserRole.builder()

                        .user(user)

                        .role(role)

                        .build();

        user.getUserRoles().add(userRole);

        if (mobileProven) {
            // The registration OTP for this exact number was just consumed: ownership is proven.
            user.markMobileVerified(LocalDateTime.now());
        }

        userRepository.save(user);

        return RegisterResponse.builder()

                .id(user.getId())

                .username(user.getUsername())

                .firstName(user.getFirstName())

                .lastName(user.getLastName())

                .build();

    }


//    @Override
//    @Transactional
//    public LoginResponse refreshToken(String refreshToken) {
//
//        UserSession session = userSessionRepository
//                .findByRefreshTokenAndRevokedFalse(refreshToken)
//                .orElseThrow(() ->
//                        new IllegalArgumentException("Invalid refresh token"));
//
//        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
//            throw new IllegalArgumentException("Refresh token expired");
//        }
//
//        User user = session.getUser();
//
//        GarageUserPrincipal principal =
//                GarageUserPrincipal.builder()
//
//                        .id(user.getId())
//
//                        .garageId(user.getGarageId())
//
//                        .username(user.getUsername())
//
//                        .password(user.getPassword())
//
//                        .firstName(user.getFirstName())
//
//                        .lastName(user.getLastName())
//
//                        .email(user.getEmail())
//
//                        .mobile(user.getMobile())
//
//                        .status(user.getStatus())
//
//                        .firstLogin(user.getFirstLogin())
//
//                        .roles(
//                                user.getUserRoles()
//                                        .stream()
//                                        .map(userRole -> userRole.getRole().getCode().name())
//                                        .collect(java.util.stream.Collectors.toSet())
//                        )
//
//                        .permissions(
//                                user.getUserRoles()
//                                        .stream()
//                                        .flatMap(userRole ->
//                                                userRole.getRole()
//                                                        .getRolePermissions()
//                                                        .stream())
//                                        .map(rolePermission ->
//                                                rolePermission.getPermission()
//                                                        .getCode()
//                                                        .name())
//                                        .collect(java.util.stream.Collectors.toSet())
//                        )
//
//                        .authorities(
//                                user.getUserRoles()
//                                        .stream()
//                                        .flatMap(userRole ->
//                                                java.util.stream.Stream.concat(
//
//                                                        java.util.stream.Stream.of(
//                                                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
//                                                                        "ROLE_" + userRole.getRole().getCode().name()
//                                                                )
//                                                        ),
//
//                                                        userRole.getRole()
//                                                                .getRolePermissions()
//                                                                .stream()
//                                                                .map(permission ->
//                                                                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
//                                                                                permission.getPermission()
//                                                                                        .getCode()
//                                                                                        .name()
//                                                                        )
//                                                                )
//                                                ))
//                                        .collect(java.util.stream.Collectors.toSet())
//                        )
//
//                        .build();
//
//        String accessToken =
//                jwtService.generateAccessToken(principal);
//
//        UserProfileResponse profile =
//                buildUserProfile(principal);
//
//        return LoginResponse.builder()
//
//                .accessToken(accessToken)
//
//                .refreshToken(refreshToken)
//
//                .expiresIn(
//                        jwtService.getAccessTokenExpiration().toMillis()
//                )
//
//                .firstLogin(principal.getFirstLogin())
//
//                .user(profile)
//
//                .build();
//    }
}