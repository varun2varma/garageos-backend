package com.garageos.modules.qualitycheck.service;

import com.garageos.core.enums.garagemembership.GarageMembershipStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.modules.garagemembership.entity.GarageMembership;
import com.garageos.modules.garagemembership.repository.GarageMembershipRepository;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Who may be recorded as the inspector of a Quality Check. Eligibility is decided server-side from persisted data:
 * an ACTIVE employee who belongs to the job card garage (their own garage, or an active membership for an owner
 * with several garages) and holds Owner, Manager, Service Advisor or Technician. A customer, driver, cashier,
 * accountant or inventory manager is not eligible, and neither is anyone from another garage.
 *
 * Model note: the identity model represents Owners and Managers as ordinary users with role codes, so they are
 * selectable like any other employee; no separate personnel model exists or is introduced.
 */
@Service
@RequiredArgsConstructor
public class QualityInspectorService {

    public static final Set<RoleCode> ELIGIBLE_ROLES =
            EnumSet.of(RoleCode.OWNER, RoleCode.MANAGER, RoleCode.SERVICE_ADVISOR, RoleCode.TECHNICIAN);

    private final UserRepository userRepository;
    private final GarageMembershipRepository membershipRepository;

    /** One selectable inspector. */
    public record Option(Long id, String name, List<String> roles) {
    }

    @Transactional(readOnly = true)
    public List<Option> listEligible(Long garageId) {

        List<Option> out = new ArrayList<>();

        for (User user : garageUsers(garageId).values()) {
            if (isEligible(user)) {
                out.add(new Option(user.getId(), displayName(user), eligibleRoleNames(user)));
            }
        }

        out.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return out;
    }

    /** @throws BusinessException (QUALITY_INSPECTOR_INELIGIBLE) unless the user may inspect for this garage */
    @Transactional(readOnly = true)
    public User requireEligible(Long userId, Long garageId) {

        User user = userId == null ? null : garageUsers(garageId).get(userId);

        // One message for unknown, other garage and wrong role: nothing is revealed about other tenants.
        if (user == null || !isEligible(user)) {
            throw new BusinessException(
                    "The selected inspector is not an eligible employee of this garage.",
                    "QUALITY_INSPECTOR_INELIGIBLE");
        }

        return user;
    }

    public static String displayName(User user) {
        return ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
    }

    private Map<Long, User> garageUsers(Long garageId) {

        Map<Long, User> users = new LinkedHashMap<>();

        if (garageId == null) {
            return users;
        }

        for (User u : userRepository.findByGarageIdIn(Set.of(garageId))) {
            users.put(u.getId(), u);
        }

        for (GarageMembership m : membershipRepository.findByGarage_IdAndStatus(garageId, GarageMembershipStatus.ACTIVE)) {
            if (m.getUser() != null) {
                users.putIfAbsent(m.getUser().getId(), m.getUser());
            }
        }

        return users;
    }

    private boolean isEligible(User user) {
        return user.getStatus() == UserStatus.ACTIVE && !eligibleRoleNames(user).isEmpty();
    }

    private List<String> eligibleRoleNames(User user) {

        List<String> roles = new ArrayList<>();

        user.getUserRoles().forEach(ur -> {
            RoleCode code = ur.getRole().getCode();
            if (ELIGIBLE_ROLES.contains(code)) {
                roles.add(code.name());
            }
        });

        return roles;
    }
}
