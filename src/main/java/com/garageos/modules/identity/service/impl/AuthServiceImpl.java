package com.garageos.modules.identity.service.impl;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.identity.dto.request.ChangePasswordRequest;
import com.garageos.modules.identity.dto.request.LoginRequest;
import com.garageos.modules.identity.dto.request.RegisterRequest;
import com.garageos.modules.identity.dto.response.LoginResponse;
import com.garageos.modules.identity.dto.response.RegisterResponse;
import com.garageos.modules.identity.dto.response.UserProfileResponse;
import com.garageos.modules.identity.entity.Role;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.entity.UserRole;
import com.garageos.modules.identity.entity.UserSession;
import com.garageos.modules.identity.repository.RoleRepository;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.repository.UserSessionRepository;
import com.garageos.modules.identity.security.jwt.JwtService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.identity.security.service.GarageUserDetailsService;
import com.garageos.modules.identity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final UserRepository userRepository;

    private final UserSessionRepository userSessionRepository;

    private final GarageUserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;

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

                .expiresIn(jwtService.getAccessTokenExpiration().toMillis())

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
    public LoginResponse refreshToken(String refreshToken) {

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
                .expiresIn(jwtService.getAccessTokenExpiration().toMillis())
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