package com.garageos.modules.identity.service.impl;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.identity.dto.request.CreateUserRequest;
import com.garageos.modules.identity.dto.request.UpdateUserRequest;
import com.garageos.modules.identity.dto.response.UserResponse;
import com.garageos.modules.identity.entity.Role;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.entity.UserRole;
import com.garageos.modules.identity.mapper.UserMapper;
import com.garageos.modules.identity.repository.RoleRepository;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.repository.UserRoleRepository;
import com.garageos.modules.identity.service.UserService;
import com.garageos.core.security.GarageScope;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.core.enums.audit.AuditEventType;
import com.garageos.core.enums.garagemembership.GarageMembershipStatus;
import com.garageos.modules.audit.service.AuditService;
import com.garageos.modules.garagemembership.repository.GarageMembershipRepository;
import com.garageos.modules.identity.repository.UserSessionRepository;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserMapper mapper;

    private final PasswordEncoder passwordEncoder;

    private final UserRoleRepository userRoleRepository;

    private final AuditService auditService;

    private final UserSessionRepository userSessionRepository;

    private final GarageMembershipRepository membershipRepository;

    /**
     * Roles a garage owner may give an employee through user administration. Never OWNER, CUSTOMER, USER or
     * SUPER_ADMIN: the platform-administrator role is granted only by the one-time, audited bootstrap
     * ({@code PlatformAdminProvisioningService}) and by nothing else.
     */
    static final Set<RoleCode> EMPLOYEE_ROLES = Set.of(
            RoleCode.MANAGER, RoleCode.SERVICE_ADVISOR, RoleCode.TECHNICIAN, RoleCode.DRIVER,
            RoleCode.INVENTORY_MANAGER, RoleCode.ACCOUNTANT, RoleCode.CASHIER);

    /** Field-injected so existing constructor-built tests are unaffected. */
    @Autowired
    private GarageScope garageScope;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        GarageUserPrincipal principal = garageScope.principal();

        boolean platformAdmin = garageScope.hasRole(principal, RoleCode.SUPER_ADMIN);

        if (!platformAdmin && !garageScope.hasRole(principal, RoleCode.OWNER)) {
            throw new AccessDeniedException("Only a garage owner or the platform administrator can create users.");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(
                    "Username already exists : " + request.getUsername());
        }

        // The garage always comes from the caller's authority, never from the request alone.
        Long garageId = request.getGarageId();

        if (!platformAdmin) {
            garageId = garageId == null ? principal.getGarageId() : garageId;

            if (garageId == null || !garageScope.canAccessGarage(principal, garageId)) {
                throw new ResourceNotFoundException("Garage not found with id : " + garageId);
            }
        }

        List<Role> roles = List.of();

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {

            roles = roleRepository.findAllById(request.getRoleIds());

            if (roles.size() != request.getRoleIds().size()) {
                throw new BusinessException("One or more roles are invalid.");
            }

            for (Role role : roles) {
                boolean allowed = EMPLOYEE_ROLES.contains(role.getCode())
                        || (platformAdmin && role.getCode() == RoleCode.OWNER);

                if (!allowed) {
                    throw new AccessDeniedException("This role cannot be assigned through user administration.");
                }
            }
        }

        User user = mapper.toEntity(request);

        user.setGarageId(garageId);

        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword()));

        user.setStatus(UserStatus.ACTIVE);

        user.setFirstLogin(true);

        user = userRepository.save(user);

        if (!roles.isEmpty()) {

            User finalUser = user;
            List<UserRole> userRoles = roles.stream()
                    .map(role -> UserRole.builder().user(finalUser).role(role).build())
                    .toList();

            userRoleRepository.saveAll(userRoles);
            user.setUserRoles(userRoles);
        }

        auditService.record(
                AuditEventType.USER_CREATED, "USER", user.getId(), user.getGarageId(),
                Map.of("roles", roles.stream().map(r -> r.getCode().name()).sorted().toList()));

        return buildResponse(user);
    }

    private UserResponse buildResponse(User user) {

        return UserResponse.builder()

                .id(user.getId())

                .garageId(user.getGarageId())

                .username(user.getUsername())

                .firstName(user.getFirstName())

                .lastName(user.getLastName())

                .email(user.getEmail())

                .mobile(user.getMobile())

                .status(user.getStatus().name())

                .firstLogin(user.getFirstLogin())

                .roles(

                        user.getUserRoles()

                                .stream()

                                .map(userRole ->
                                        userRole
                                                .getRole()
                                                .getCode()
                                                .name())

                                .sorted()

                                .toList()

                )

                .build();

    }

    /**
     * Users are tenant data: a caller may only touch themselves or users
     * of a garage they can access. Anything else is reported as not found.
     */
    private User requireAccessibleUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found : " + id));

        GarageUserPrincipal principal = garageScope.principal();

        if (garageScope.isUnrestricted(principal)
                || id.equals(principal.getId())
                || garageScope.canAccessGarage(principal, user.getGarageId())) {
            return user;
        }

        throw new ResourceNotFoundException("User not found : " + id);
    }

    private List<User> accessibleGarageUsers() {

        GarageUserPrincipal principal = garageScope.principal();

        if (garageScope.isUnrestricted(principal)) {
            return userRepository.findAll();
        }

        java.util.Set<Long> garageIds = garageScope.allowedGarageIds(principal);

        return garageIds.isEmpty()
                ? List.of()
                : userRepository.findByGarageIdIn(garageIds);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {

        User user = requireAccessibleUser(id);

        return buildResponse(user);
    }

    /**
     * Soft delete: the row is kept (job assignments, audit history and financial records reference users) but the
     * account can no longer sign in, its sessions are revoked and its garage memberships end. Only an owner (of the
     * user's garage) or the platform administrator may do it; nobody deletes themselves here (that is
     * {@code DELETE /auth/account}); nobody but the platform administrator deletes an owner; a platform
     * administrator account is never deleted through this endpoint.
     */
    @Override
    @Transactional
    public void deleteUser(Long id) {

        GarageUserPrincipal principal = garageScope.principal();

        requireUserAdministrator(principal);

        User user = requireAccessibleUser(id);

        if (id.equals(principal.getId())) {
            throw new AccessDeniedException("Use account deletion to remove your own account.");
        }

        requireMayManage(principal, user);

        if (user.getStatus() == UserStatus.DELETED) {
            return;
        }

        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);

        userSessionRepository.revokeAllByUserId(user.getId());

        membershipRepository.findByUser_Id(user.getId()).stream()
                .filter(m -> m.getStatus() == GarageMembershipStatus.ACTIVE
                        || m.getStatus() == GarageMembershipStatus.PENDING)
                .forEach(m -> {
                    m.setStatus(GarageMembershipStatus.REMOVED);
                    membershipRepository.save(m);
                });

        auditService.record(AuditEventType.USER_DELETED, "USER", user.getId(), user.getGarageId(), Map.of());
    }

    private void requireUserAdministrator(GarageUserPrincipal principal) {

        if (!garageScope.hasRole(principal, RoleCode.SUPER_ADMIN) && !garageScope.hasRole(principal, RoleCode.OWNER)) {
            throw new AccessDeniedException("Only a garage owner or the platform administrator can do this.");
        }
    }

    /**
     * An owner manages employees. Another owner, and any platform administrator, are out of reach for an owner:
     * only the platform administrator manages owners, and a platform-administrator account is never managed here.
     */
    private void requireMayManage(GarageUserPrincipal principal, User target) {

        if (userRoleRepository.existsByUserIdAndRoleCode(target.getId(), RoleCode.SUPER_ADMIN)) {
            throw new AccessDeniedException("This account cannot be managed through user administration.");
        }

        if (userRoleRepository.existsByUserIdAndRoleCode(target.getId(), RoleCode.OWNER)
                && !garageScope.hasRole(principal, RoleCode.SUPER_ADMIN)) {
            throw new AccessDeniedException("Only the platform administrator can manage an owner account.");
        }
    }

    @Override
    public Page<UserResponse> getAllUsers(int page,
                                          int size,
                                          String sortBy,
                                          String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable =
                PageRequest.of(page, size, sort);

        GarageUserPrincipal principal = garageScope.principal();

        Page<User> users;

        if (garageScope.isUnrestricted(principal)) {
            users = userRepository.findAll(pageable);
        } else {
            java.util.Set<Long> garageIds = garageScope.allowedGarageIds(principal);
            users = garageIds.isEmpty()
                    ? Page.empty(pageable)
                    : userRepository.findByGarageIdIn(garageIds, pageable);
        }

        return users.map(this::buildResponse);
    }

    /**
     * Profile update. Roles are NOT changed here (a request carrying role ids is refused), the mobile number is the
     * customer-identity key and is not changed here either, and the account status is changed only by an
     * administrator for someone else. A user may edit their own name and e-mail; editing another user needs an
     * owner (of that user's garage) or the platform administrator.
     */
    @Override
    @Transactional
    public UserResponse updateUser(
            Long id,
            UpdateUserRequest request) {

        GarageUserPrincipal principal = garageScope.principal();

        User user = requireAccessibleUser(id);

        boolean self = id.equals(principal.getId());

        if (request.getRoleIds() != null) {
            throw new BusinessException("Roles cannot be changed through this endpoint.");
        }

        if (request.getMobile() != null && !request.getMobile().equals(user.getMobile())) {
            throw new BusinessException("The mobile number cannot be changed here.");
        }

        if (!self) {
            requireUserAdministrator(principal);
            requireMayManage(principal, user);
        }

        if (request.getActive() != null) {

            boolean currentlyActive = user.getStatus() == UserStatus.ACTIVE;

            if (request.getActive() != currentlyActive) {

                if (self) {
                    throw new AccessDeniedException("You cannot change your own account status.");
                }

                if (user.getStatus() == UserStatus.DELETED) {
                    throw new BusinessException("A deleted account cannot be changed.");
                }

                user.setStatus(request.getActive() ? UserStatus.ACTIVE : UserStatus.INACTIVE);

                if (!request.getActive()) {
                    userSessionRepository.revokeAllByUserId(user.getId());
                }
            }
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {

            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("Email already exists.");
            }

            user.setEmail(request.getEmail());
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        user = userRepository.save(user);

        auditService.record(AuditEventType.USER_UPDATED, "USER", user.getId(), user.getGarageId(),
                Map.of("self", self));

        return buildResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getTechnicians() {

        return accessibleGarageUsers()
                .stream()

                .filter(user ->

                        user.getUserRoles()

                                .stream()

                                .anyMatch(role ->

                                        role.getRole()
                                                .getCode()
                                                .name()
                                                .equals("TECHNICIAN")

                                )
                )

                .map(this::buildResponse)

                .toList();

    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getRepairEmployees() {

        return accessibleGarageUsers()
                .stream()

                .filter(user ->
                        user.getUserRoles()
                                .stream()
                                .anyMatch(userRole -> {

                                    String role =
                                            userRole
                                                    .getRole()
                                                    .getCode()
                                                    .name();

                                    return role.equals("TECHNICIAN")
                                            || role.equals("DRIVER");

                                })
                )

                .map(this::buildResponse)

                .toList();

    }


    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getDrivers(
            Long garageId) {

        if (garageId == null) {
            throw new BusinessException(
                    "Garage ID is required to fetch drivers."
            );
        }

        return userRepository
                .findByGarageIdAndRoleAndStatus(
                        garageId,
                        RoleCode.DRIVER,
                        UserStatus.ACTIVE
                )
                .stream()
                .map(this::buildResponse)
                .toList();
    }


}
