package com.garageos.modules.identity.service.impl;

import com.garageos.core.enums.audit.AuditEventType;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.modules.audit.service.AuditService;
import com.garageos.modules.identity.entity.Role;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.entity.UserRole;
import com.garageos.modules.identity.repository.RoleRepository;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.repository.UserRoleRepository;
import com.garageos.modules.identity.service.PlatformAdminProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Safety conditions for the first platform administrator (all must hold, otherwise nothing changes):
 * <ul>
 *   <li>the operator declared an environment and it is one of the active Spring profiles
 *       (so a misconfigured or default-profile run can never grant it);</li>
 *   <li>the username is confirmed a second time;</li>
 *   <li>no platform administrator exists yet (a one-time grant, never repeated by a restart);</li>
 *   <li>the account exists, is ACTIVE, belongs to no garage and holds no role other than the plain
 *       registration role USER - so a garage owner, employee or customer is never promoted and
 *       platform administration stays separate from garage ownership.</li>
 * </ul>
 * Uses the existing role tables; the audit trail records the grant without any secret.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformAdminProvisioningServiceImpl implements PlatformAdminProvisioningService {

    private static final Set<RoleCode> ALLOWED_EXISTING_ROLES = Set.of(RoleCode.USER);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuditService auditService;
    private final Environment environment;

    @Override
    @Transactional
    public Outcome provision(String username, String confirmUsername, String declaredEnvironment) {

        if (isBlank(username) || isBlank(confirmUsername) || isBlank(declaredEnvironment)) {
            throw new BusinessException(
                    "Platform admin bootstrap needs username, confirm-username and environment.");
        }

        if (!username.trim().equals(confirmUsername.trim())) {
            throw new BusinessException("Platform admin bootstrap: username and confirm-username differ.");
        }

        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());

        if (activeProfiles.isEmpty() || !activeProfiles.contains(declaredEnvironment.trim())) {
            throw new BusinessException(
                    "Platform admin bootstrap: the declared environment is not an active Spring profile.");
        }

        if (userRoleRepository.existsByRoleCode(RoleCode.SUPER_ADMIN)) {
            return Outcome.ALREADY_PROVISIONED;
        }

        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new BusinessException(
                        "Platform admin bootstrap: the account must be registered first."));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Platform admin bootstrap: the account is not active.");
        }

        if (user.getGarageId() != null) {
            throw new BusinessException(
                    "Platform admin bootstrap: the account belongs to a garage. Register a separate account.");
        }

        List<UserRole> existing = userRoleRepository.findByUserId(user.getId());

        boolean onlyPlainRole = existing.stream()
                .map(r -> r.getRole().getCode())
                .allMatch(ALLOWED_EXISTING_ROLES::contains);

        if (!onlyPlainRole) {
            throw new BusinessException(
                    "Platform admin bootstrap: the account already holds garage or customer roles. "
                            + "Register a separate account for platform administration.");
        }

        Role superAdmin = roleRepository.findByCode(RoleCode.SUPER_ADMIN)
                .orElseThrow(() -> new BusinessException("Platform admin bootstrap: the SUPER_ADMIN role is missing."));

        UserRole grant = UserRole.builder().user(user).role(superAdmin).build();
        userRoleRepository.save(grant);

        auditService.record(
                AuditEventType.PLATFORM_ADMIN_GRANTED,
                "USER",
                user.getId(),
                null,
                Map.of("mechanism", "startup-bootstrap", "environment", declaredEnvironment.trim()));

        log.warn("[PLATFORM-ADMIN] SUPER_ADMIN granted to userId={} in environment={}. "
                + "Disable the bootstrap configuration now.", user.getId(), declaredEnvironment.trim());

        return Outcome.GRANTED;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
