package com.garageos.modules.identity.service.impl;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.identity.dto.request.ChangePasswordRequest;
import com.garageos.modules.identity.dto.request.ForgotPasswordRequest;
import com.garageos.modules.identity.dto.request.LoginRequest;
import com.garageos.modules.identity.dto.request.RegisterRequest;
import com.garageos.modules.identity.dto.request.ResetPasswordRequest;
import com.garageos.modules.identity.dto.response.LoginResponse;
import com.garageos.modules.identity.dto.response.RegisterResponse;
import com.garageos.modules.identity.dto.response.UserProfileResponse;
import com.garageos.modules.identity.entity.PasswordResetToken;
import com.garageos.modules.identity.entity.Role;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.entity.UserRole;
import com.garageos.modules.identity.entity.UserSession;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int RESET_TOKEN_LENGTH_BYTES = 24;
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 30;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final UserRepository userRepository;

    private final UserSessionRepository userSessionRepository;

    private final GarageUserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final PasswordResetNotificationService passwordResetNotificationService;

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
    public void forgotPassword(ForgotPasswordRequest request) {

        String identifier = request.getIdentifier().trim();

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .or(() -> userRepository.findByMobile(identifier))
                .orElse(null);

        // Deliberately does not throw when no account matches - revealing
        // "no such account" here is an account-enumeration vector. The
        // caller always sees the same generic outcome either way; only
        // the (matched) user's own inbox/phone would ever see a
        // difference, once real delivery is configured.
        if (user == null) {
            log.debug("Password reset requested for unknown identifier.");
            return;
        }

        // Any previously-issued, still-unused token for this user is
        // superseded - only the newest one should be usable, matching
        // VehicleHandover's expireExistingActiveCode pattern.
        for (PasswordResetToken existing : passwordResetTokenRepository.findByUserIdAndUsedFalse(user.getId())) {
            existing.setUsed(true);
            passwordResetTokenRepository.save(existing);
        }

        byte[] randomBytes = new byte[RESET_TOKEN_LENGTH_BYTES];
        secureRandom.nextBytes(randomBytes);
        String plaintextToken = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        PasswordResetToken token = PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(passwordEncoder.encode(plaintextToken))
                .expiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES))
                .used(false)
                .build();

        passwordResetTokenRepository.save(token);

        String destination = user.getEmail() != null && !user.getEmail().isBlank() ? user.getEmail() : user.getMobile();

        passwordResetNotificationService.sendResetToken(destination, plaintextToken, user.getFirstName());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        // The token can't be looked up by a direct query (only the hash
        // is stored, same reasoning as VehicleHandover's codeHash) - scan
        // not-yet-used, not-yet-expired tokens and match by
        // passwordEncoder, same shape as HandoverServiceImpl.verify.
        List<PasswordResetToken> candidates = passwordResetTokenRepository.findAll().stream()
                .filter(t -> !Boolean.TRUE.equals(t.getUsed()))
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .toList();

        PasswordResetToken matched = candidates.stream()
                .filter(t -> passwordEncoder.matches(request.getToken(), t.getTokenHash()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "This reset link is invalid or has expired. Please request a new one."));

        User user = userRepository.findById(matched.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFirstLogin(false);
        userRepository.save(user);

        matched.setUsed(true);
        passwordResetTokenRepository.save(matched);

        userSessionRepository.revokeAllByUserId(user.getId());
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