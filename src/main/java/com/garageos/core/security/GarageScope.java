package com.garageos.core.security;

import com.garageos.core.enums.garagemembership.GarageMembershipStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.modules.garagemembership.repository.GarageMembershipRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Single answer to "which garage(s) may the authenticated caller see?".
 *
 * <ul>
 *   <li>SUPER_ADMIN: unrestricted (legitimate system behaviour).</li>
 *   <li>OWNER: the garage on the principal plus every ACTIVE garage
 *       membership (multi-garage owner).</li>
 *   <li>Every other employee role: only the single garage on the principal.</li>
 *   <li>A pure customer (CUSTOMER role, no staff role) has no staff garage
 *       scope at all.</li>
 * </ul>
 *
 * Never trusts anything from the request: only the authenticated principal
 * and the garage_membership table.
 */
@Component
@RequiredArgsConstructor
public class GarageScope {

    private static final Set<String> STAFF_ROLES = Set.of(
            RoleCode.OWNER.name(),
            RoleCode.MANAGER.name(),
            RoleCode.SERVICE_ADVISOR.name(),
            RoleCode.TECHNICIAN.name(),
            RoleCode.DRIVER.name(),
            RoleCode.ACCOUNTANT.name(),
            RoleCode.CASHIER.name(),
            RoleCode.INVENTORY_MANAGER.name());

    private final GarageMembershipRepository membershipRepository;

    public GarageUserPrincipal principal() {

        Object principal = SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        if (!(principal instanceof GarageUserPrincipal garagePrincipal)) {
            throw new BusinessException("Authenticated garage user context is required.");
        }

        return garagePrincipal;
    }

    public boolean hasRole(GarageUserPrincipal principal, RoleCode role) {
        return principal.getRoles() != null && principal.getRoles().contains(role.name());
    }

    public boolean isUnrestricted(GarageUserPrincipal principal) {
        return hasRole(principal, RoleCode.SUPER_ADMIN);
    }

    public boolean isStaff(GarageUserPrincipal principal) {

        if (principal.getRoles() == null) {
            return false;
        }

        return principal.getRoles().stream().anyMatch(STAFF_ROLES::contains);
    }

    /** CUSTOMER role and no staff role: restricted to their own data. */
    public boolean isCustomerOnly(GarageUserPrincipal principal) {
        return hasRole(principal, RoleCode.CUSTOMER) && !isStaff(principal);
    }

    /** Garages the (non-unrestricted) caller may see; empty for a pure customer / no garage. */
    public Set<Long> allowedGarageIds(GarageUserPrincipal principal) {

        Set<Long> ids = new HashSet<>();

        if (principal.getGarageId() != null) {
            ids.add(principal.getGarageId());
        }

        if (hasRole(principal, RoleCode.OWNER) && principal.getId() != null) {

            membershipRepository.findByUser_Id(principal.getId()).stream()
                    .filter(m -> m.getStatus() == GarageMembershipStatus.ACTIVE)
                    .map(m -> m.getGarage().getId())
                    .forEach(ids::add);
        }

        return ids;
    }

    public boolean canAccessGarage(GarageUserPrincipal principal, Long garageId) {

        if (garageId == null) {
            return false;
        }

        if (isUnrestricted(principal)) {
            return true;
        }

        return allowedGarageIds(principal).contains(garageId);
    }

    /**
     * Membership-based access to a garage, WITHOUT the platform-administrator bypass: the garage on the principal,
     * plus (for an OWNER only) every garage the owner holds an ACTIVE membership in. A manager, technician or any
     * other employee is limited to the single garage on their own account; an owner's several garages never widen
     * another person's access. {@code scope} may be null in unit tests that build a service by hand, in which case
     * only the principal's own garage counts (the behaviour before multi-garage owners).
     */
    public static boolean isMemberOfGarage(GarageScope scope, GarageUserPrincipal principal, Long garageId) {

        if (garageId == null || principal == null) {
            return false;
        }

        if (scope == null) {
            return garageId.equals(principal.getGarageId());
        }

        return scope.allowedGarageIds(principal).contains(garageId);
    }

    /**
     * Read access to anything that hangs off a job card: staff of the job card's garage (membership model), a
     * platform administrator, or the customer the job card belongs to. Anyone else, or a caller with no
     * authenticated principal, gets the supplied not-found message (another tenant's data is never confirmed).
     */
    public static void requireJobCardRead(
            GarageScope scope, com.garageos.modules.jobcard.entity.JobCard jobCard, String notFound) {

        GarageUserPrincipal principal = currentOrNull();

        if (principal != null && scope != null && scope.isUnrestricted(principal)) {
            return;
        }

        boolean staff = principal != null
                && jobCard != null
                && jobCard.getGarage() != null
                && isMemberOfGarage(scope, principal, jobCard.getGarage().getId());

        boolean ownCustomer = principal != null
                && jobCard != null
                && jobCard.getCustomer() != null
                && isOwnCustomer(principal, jobCard.getCustomer().getMobileNumber());

        if (!staff && !ownCustomer) {
            throw new com.garageos.core.exception.ResourceNotFoundException(notFound);
        }
    }

    /** Write access: staff of the job card's garage only (never a customer, never the platform administrator). */
    public static void requireJobCardStaff(
            GarageScope scope, com.garageos.modules.jobcard.entity.JobCard jobCard, String notFound) {

        GarageUserPrincipal principal = currentOrNull();

        if (principal == null
                || jobCard == null
                || jobCard.getGarage() == null
                || !isMemberOfGarage(scope, principal, jobCard.getGarage().getId())) {
            throw new com.garageos.core.exception.ResourceNotFoundException(notFound);
        }
    }

    /** The authenticated garage principal, or null when there is none (system thread, unauthenticated). */
    public static GarageUserPrincipal currentOrNull() {

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null && authentication.getPrincipal() instanceof GarageUserPrincipal p ? p : null;
    }

    /** True when the principal's registered mobile number is the given customer's mobile number. */
    public static boolean isOwnCustomer(GarageUserPrincipal principal, String customerMobile) {

        return principal != null
                && principal.getMobile() != null
                && customerMobile != null
                && principal.getMobile().equals(customerMobile);
    }

    /**
     * The garage for a single-garage operation: the explicitly requested one
     * if the caller may access it, otherwise the principal's own garage.
     */
    public Long resolveGarageId(GarageUserPrincipal principal, Long requestedGarageId) {

        if (requestedGarageId != null) {

            if (!canAccessGarage(principal, requestedGarageId)) {
                throw new BusinessException("This garage does not belong to you.");
            }

            return requestedGarageId;
        }

        if (principal.getGarageId() == null) {
            throw new BusinessException("No garage context for this account.");
        }

        return principal.getGarageId();
    }
}
