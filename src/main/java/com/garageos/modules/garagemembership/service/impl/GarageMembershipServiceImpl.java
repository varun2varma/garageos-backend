package com.garageos.modules.garagemembership.service.impl;

import com.garageos.core.enums.garagemembership.GarageMembershipStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.garagemembership.dto.request.ApproveGarageMembershipRequest;
import com.garageos.modules.garagemembership.dto.request.JoinGarageRequest;
import com.garageos.modules.garagemembership.dto.response.GarageMembershipResponse;
import com.garageos.modules.garagemembership.entity.GarageMembership;
import com.garageos.modules.garagemembership.mapper.GarageMembershipMapper;
import com.garageos.modules.garagemembership.repository.GarageMembershipRepository;
import com.garageos.modules.garagemembership.service.GarageMembershipService;
import com.garageos.modules.identity.entity.Role;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.entity.UserRole;
import com.garageos.modules.identity.repository.RoleRepository;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class GarageMembershipServiceImpl
        implements GarageMembershipService {

    private final GarageMembershipRepository membershipRepository;
    private final GarageRepository garageRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final GarageMembershipMapper mapper;

    @Override
    public GarageMembershipResponse joinGarage(
            Long userId,
            JoinGarageRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found."));

        Garage garage = garageRepository
                .findByGarageCode(request.getGarageCode())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Garage not found."));

        /*
         * Corrective fix: this previously blocked on the mere EXISTENCE of
         * a membership row, with no regard for its status. Removing an
         * employee does not delete their row - it flips the status - so
         * once someone had been removed from a garage they could never
         * join it again: every attempt hit "You have already requested to
         * join this garage."
         *
         * Only an application that is genuinely still live should block a
         * new one. A membership in any terminal state (REJECTED, REMOVED,
         * INACTIVE, SUSPENDED) is a closed chapter and must not stand in
         * the way of re-applying.
         */
        GarageMembership membership =
                membershipRepository
                        .findByGarage_IdAndUser_Id(garage.getId(), user.getId())
                        .orElse(null);

        if (membership != null) {

            if (membership.getStatus() == GarageMembershipStatus.PENDING) {

                throw new BusinessException(
                        "You have already requested to join this garage. "
                                + "Your request is awaiting approval.");
            }

            if (membership.getStatus() == GarageMembershipStatus.ACTIVE) {

                throw new BusinessException(
                        "You are already a member of this garage.");
            }

            /*
             * Reactivation rather than a second row: the table carries a
             * unique index on (garage_id, user_id) - uk_garage_membership,
             * V24 - so a fresh insert would violate it. Reusing the row
             * keeps the constraint intact and needs no migration.
             *
             * Every trace of the previous membership is cleared, so the
             * new request is a genuinely fresh PENDING application and
             * cannot inherit a stale approval, approver, employee code or
             * rejection remark from the last one.
             */
            membership.setStatus(GarageMembershipStatus.PENDING);
            membership.setJoinedAt(LocalDateTime.now());
            membership.setApprovedAt(null);
            membership.setApprovedBy(null);
            membership.setEmployeeCode(null);
            membership.setRemarks(null);

            return buildResponse(membershipRepository.save(membership));
        }

        membership = new GarageMembership();

        membership.setGarage(garage);

        membership.setUser(user);

        membership.setStatus(
                GarageMembershipStatus.PENDING);

        membership.setJoinedAt(LocalDateTime.now());

        membership =
                membershipRepository.save(membership);

        return buildResponse(membership);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GarageMembershipResponse> getPendingMemberships(
            Long garageId) {

        return membershipRepository
                .findByGarage_IdAndStatus(
                        garageId,
                        GarageMembershipStatus.PENDING
                )
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GarageMembershipResponse> getEmployees(
            Long garageId) {

        return membershipRepository
                .findByGarage_IdAndStatus(
                        garageId,
                        GarageMembershipStatus.ACTIVE
                )
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GarageMembershipResponse> getMyMemberships(
            Long userId) {

        return membershipRepository
                .findByUser_Id(userId)
                .stream()
                .filter(m -> m.getStatus() == GarageMembershipStatus.ACTIVE)
                .map(this::buildResponse)
                .toList();
    }

    @Override
    public GarageMembershipResponse approveMembership(
            Long membershipId,
            Long ownerId,
            ApproveGarageMembershipRequest request) {

        GarageMembership membership =
                membershipRepository.findById(membershipId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Membership not found."));

        if (membership.getStatus() !=
                GarageMembershipStatus.PENDING) {

            throw new BusinessException(
                    "Membership is already processed.");
        }

        User owner =
                userRepository.findById(ownerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Owner not found."));

        User employee = membership.getUser();

        for (Long roleId : request.getRoleIds()) {

            Role role =
                    roleRepository.findById(roleId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Role not found."));

            if (!userRoleRepository.existsByUserIdAndRoleCode(
                    employee.getId(),
                    role.getCode())) {

                UserRole userRole =
                        UserRole.builder()
                                .user(employee)
                                .role(role)
                                .build();

                userRoleRepository.save(userRole);

            }

        }

        String employeeCode = generateEmployeeCode(
                membership.getGarage().getId());

        employee.setGarageId(
                membership.getGarage().getId());

        employee.setEmployeeCode(employeeCode);

        userRepository.save(employee);

        membership.setStatus(
                GarageMembershipStatus.ACTIVE);

        membership.setApprovedAt(
                LocalDateTime.now());

        membership.setApprovedBy(owner);

        membership.setEmployeeCode(employeeCode);

        membershipRepository.save(membership);

        return buildResponse(membership);
    }

    /**
     * Concurrency-safe replacement for the previous MAX(existing code) + 1
     * scan, which raced when two employees were approved into the same
     * garage at the same time. Garage.nextEmployeeSequence is read and
     * incremented under a pessimistic row lock (held for the remainder of
     * this @Transactional method), so concurrent approvals for the same
     * garage serialize instead of computing the same next value.
     */
    private String generateEmployeeCode(Long garageId) {

        Garage garage = garageRepository.findByIdForUpdate(garageId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Garage not found."));

        int sequence = garage.getNextEmployeeSequence();

        garage.setNextEmployeeSequence(sequence + 1);

        garageRepository.save(garage);

        return String.format(
                "%s-EMP%03d",
                garage.getGarageCode(),
                sequence
        );
    }

    @Override
    public GarageMembershipResponse rejectMembership(
            Long membershipId,
            String remarks) {

        GarageMembership membership =
                membershipRepository.findById(membershipId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Membership not found."));

        if (membership.getStatus() !=
                GarageMembershipStatus.PENDING) {

            throw new BusinessException(
                    "Only pending membership can be rejected.");
        }

        membership.setStatus(
                GarageMembershipStatus.REJECTED);

        membership.setRemarks(remarks);

        membershipRepository.save(membership);

        return buildResponse(membership);
    }

    /**
     * Removing someone from a garage revokes the access that garage gave
     * them. It is not an account deletion and not an identity change.
     *
     * Corrective fix: this called userRoleRepository.deleteByUserId(),
     * which wiped *every* global role the user had - including the base
     * USER role granted at registration. The account was left with no
     * roles at all, so the app's role-based home resolution fell through
     * to the onboarding screen, whose "Customer Portal" option is the
     * only one that leads anywhere without a garage. That is how a
     * removed employee ended up inside the customer flow: not a
     * deliberate product rule, but the byproduct of a role wipe.
     *
     * Now only the garage-granted operational roles are revoked. The base
     * USER role is preserved (and restored if a previous removal had
     * already wiped it), and a CUSTOMER role is left alone if the person
     * genuinely holds one - being an employee and being a customer are
     * separate identities, and removing one must not silently create or
     * destroy the other.
     */
    @Override
    public void removeMembership(Long membershipId) {

        GarageMembership membership =
                membershipRepository.findById(membershipId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Membership not found."));

        User user = membership.getUser();

        user.setGarageId(null);

        user.setEmployeeCode(null);

        userRepository.save(user);

        revokeGarageOperationalRoles(user);

        membership.setStatus(
                GarageMembershipStatus.REMOVED
        );

        membershipRepository.save(membership);

    }

    /**
     * The roles a garage grants an employee when it approves them. These
     * are the only roles a removal takes away.
     *
     * USER is the base identity role every account is registered with and
     * is never garage-granted. CUSTOMER is a separate self-service
     * identity. SUPER_ADMIN is platform-level. None of them are a
     * garage's to revoke.
     */
    private static final Set<RoleCode> GARAGE_OPERATIONAL_ROLES = EnumSet.of(
            RoleCode.OWNER,
            RoleCode.MANAGER,
            RoleCode.SERVICE_ADVISOR,
            RoleCode.TECHNICIAN,
            RoleCode.DRIVER,
            RoleCode.INVENTORY_MANAGER,
            RoleCode.ACCOUNTANT,
            RoleCode.CASHIER
    );

    private void revokeGarageOperationalRoles(User user) {

        List<UserRole> toRevoke =
                userRoleRepository.findByUserId(user.getId())
                        .stream()
                        .filter(userRole -> GARAGE_OPERATIONAL_ROLES
                                .contains(userRole.getRole().getCode()))
                        .toList();

        userRoleRepository.deleteAll(toRevoke);

        // Self-healing: an account removed before this fix had its USER
        // role deleted too, leaving it with no identity at all. Restore it
        // so the account is a normal signed-up user again rather than a
        // role-less one that falls through to the customer flow.
        if (!userRoleRepository.existsByUserIdAndRoleCode(
                user.getId(), RoleCode.USER)) {

            roleRepository.findByCode(RoleCode.USER).ifPresent(baseRole ->
                    userRoleRepository.save(
                            UserRole.builder()
                                    .user(user)
                                    .role(baseRole)
                                    .build()));
        }
    }

    private GarageMembershipResponse buildResponse(
            GarageMembership membership) {

        System.out.println("========== MEMBERSHIP ==========");
        System.out.println("User Id      : " + membership.getUser().getId());
        System.out.println("First Name   : " + membership.getUser().getFirstName());
        System.out.println("Last Name    : " + membership.getUser().getLastName());
        System.out.println("Mobile       : " + membership.getUser().getMobile());
        System.out.println("Email        : " + membership.getUser().getEmail());
        System.out.println("================================");

        GarageMembershipResponse response =
                mapper.toResponse(membership);

        List<String> roles =
                userRoleRepository.findByUserId(
                                membership.getUser().getId())
                        .stream()
                        .map(userRole ->
                                userRole.getRole()
                                        .getCode()
                                        .name())
                        .filter(role -> !"USER".equals(role))
                        .sorted()
                        .toList();

        response.setRoles(roles);

        return response;
    }

}
