package com.garageos.modules.garagemembership.service.impl;

import com.garageos.core.enums.garagemembership.GarageMembershipStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.garagemembership.dto.request.JoinGarageRequest;
import com.garageos.modules.garagemembership.entity.GarageMembership;
import com.garageos.modules.garagemembership.mapper.GarageMembershipMapper;
import com.garageos.modules.garagemembership.repository.GarageMembershipRepository;
import com.garageos.modules.identity.entity.Role;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.entity.UserRole;
import com.garageos.modules.identity.repository.RoleRepository;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Garage membership lifecycle.
 *
 * Two defects are pinned here:
 *
 * Bug #2 - joinGarage() blocked on the mere EXISTENCE of a membership
 * row, ignoring its status. Removing an employee flips the status rather
 * than deleting the row, so a removed employee could never rejoin: every
 * attempt hit "You have already requested to join this garage."
 *
 * Bug #3 - removeMembership() called deleteByUserId(), wiping every
 * global role including the base USER role granted at registration. The
 * account was left with no roles at all, which is what pushed removed
 * employees into the customer flow.
 */
@ExtendWith(MockitoExtension.class)
class GarageMembershipLifecycleTest {

    @Mock private GarageMembershipRepository membershipRepository;
    @Mock private GarageRepository garageRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private GarageMembershipMapper mapper;

    private static final Long GARAGE_ID = 10L;
    private static final Long USER_ID = 5L;
    private static final String GARAGE_CODE = "G010";

    private Garage garage;
    private User user;

    private GarageMembershipServiceImpl service() {
        return new GarageMembershipServiceImpl(
                membershipRepository,
                garageRepository,
                userRepository,
                roleRepository,
                userRoleRepository,
                mapper
        );
    }

    @BeforeEach
    void setUp() {
        garage = new Garage();
        garage.setId(GARAGE_ID);
        garage.setGarageCode(GARAGE_CODE);

        user = new User();
        user.setId(USER_ID);

        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        lenient().when(garageRepository.findByGarageCode(GARAGE_CODE))
                .thenReturn(Optional.of(garage));
        lenient().when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of());
        lenient().when(mapper.toResponse(any()))
                .thenReturn(new com.garageos.modules.garagemembership.dto.response
                        .GarageMembershipResponse());
    }

    private JoinGarageRequest joinRequest() {
        JoinGarageRequest request = new JoinGarageRequest();
        request.setGarageCode(GARAGE_CODE);
        return request;
    }

    private GarageMembership membershipWith(GarageMembershipStatus status) {
        GarageMembership membership = new GarageMembership();
        membership.setId(99L);
        membership.setGarage(garage);
        membership.setUser(user);
        membership.setStatus(status);
        return membership;
    }

    // ---- Bug #2: re-applying after removal ----

    @Test
    void join_firstTime_createsAPendingApplication() {

        when(membershipRepository.findByGarage_IdAndUser_Id(GARAGE_ID, USER_ID))
                .thenReturn(Optional.empty());
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().joinGarage(USER_ID, joinRequest());

        ArgumentCaptor<GarageMembership> captor =
                ArgumentCaptor.forClass(GarageMembership.class);
        verify(membershipRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus())
                .isEqualTo(GarageMembershipStatus.PENDING);
        assertThat(captor.getValue().getJoinedAt()).isNotNull();
    }

    @Test
    void join_afterBeingRemoved_isAllowedAndBecomesPendingAgain() {

        GarageMembership removed = membershipWith(GarageMembershipStatus.REMOVED);
        removed.setEmployeeCode("G010-EMP001");
        removed.setApprovedAt(java.time.LocalDateTime.now().minusDays(30));
        removed.setApprovedBy(user);
        removed.setRemarks("Left the company");

        when(membershipRepository.findByGarage_IdAndUser_Id(GARAGE_ID, USER_ID))
                .thenReturn(Optional.of(removed));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().joinGarage(USER_ID, joinRequest());

        assertThat(removed.getStatus()).isEqualTo(GarageMembershipStatus.PENDING);

        // A genuinely fresh application: nothing from the previous
        // membership may leak into it.
        assertThat(removed.getApprovedAt()).isNull();
        assertThat(removed.getApprovedBy()).isNull();
        assertThat(removed.getEmployeeCode()).isNull();
        assertThat(removed.getRemarks()).isNull();
        assertThat(removed.getJoinedAt()).isNotNull();
    }

    /**
     * uk_garage_membership(garage_id, user_id) is unique, so re-applying
     * must reuse the row rather than insert a second one.
     */
    @Test
    void join_afterBeingRemoved_reusesTheRowRatherThanInsertingASecond() {

        GarageMembership removed = membershipWith(GarageMembershipStatus.REMOVED);

        when(membershipRepository.findByGarage_IdAndUser_Id(GARAGE_ID, USER_ID))
                .thenReturn(Optional.of(removed));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().joinGarage(USER_ID, joinRequest());

        ArgumentCaptor<GarageMembership> captor =
                ArgumentCaptor.forClass(GarageMembership.class);
        verify(membershipRepository).save(captor.capture());

        assertThat(captor.getValue().getId()).isEqualTo(99L);
    }

    @Test
    void join_afterRejectionOrDeactivation_isAlsoAllowed() {

        for (GarageMembershipStatus terminal : Arrays.asList(
                GarageMembershipStatus.REJECTED,
                GarageMembershipStatus.REMOVED,
                GarageMembershipStatus.INACTIVE,
                GarageMembershipStatus.SUSPENDED)) {

            GarageMembership existing = membershipWith(terminal);

            when(membershipRepository.findByGarage_IdAndUser_Id(GARAGE_ID, USER_ID))
                    .thenReturn(Optional.of(existing));
            when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service().joinGarage(USER_ID, joinRequest());

            assertThat(existing.getStatus())
                    .as("%s must not block a new application", terminal)
                    .isEqualTo(GarageMembershipStatus.PENDING);
        }
    }

    // ---- Duplicate-application guards must still hold ----

    @Test
    void join_whileAlreadyPending_isRejected() {

        when(membershipRepository.findByGarage_IdAndUser_Id(GARAGE_ID, USER_ID))
                .thenReturn(Optional.of(membershipWith(GarageMembershipStatus.PENDING)));

        assertThatThrownBy(() -> service().joinGarage(USER_ID, joinRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("awaiting approval");

        verify(membershipRepository, never()).save(any());
    }

    @Test
    void join_whileAlreadyActive_isRejected() {

        when(membershipRepository.findByGarage_IdAndUser_Id(GARAGE_ID, USER_ID))
                .thenReturn(Optional.of(membershipWith(GarageMembershipStatus.ACTIVE)));

        assertThatThrownBy(() -> service().joinGarage(USER_ID, joinRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already a member");

        verify(membershipRepository, never()).save(any());
    }

    // ---- Bug #3: removal must not destroy the account's identity ----

    private UserRole userRoleOf(RoleCode code) {
        Role role = new Role();
        role.setCode(code);
        return UserRole.builder().user(user).role(role).build();
    }

    @Test
    void remove_revokesGarageRolesButKeepsTheBaseUserRole() {

        List<UserRole> roles = new ArrayList<>(List.of(
                userRoleOf(RoleCode.USER),
                userRoleOf(RoleCode.TECHNICIAN),
                userRoleOf(RoleCode.DRIVER)
        ));

        GarageMembership membership = membershipWith(GarageMembershipStatus.ACTIVE);
        when(membershipRepository.findById(99L)).thenReturn(Optional.of(membership));
        when(userRoleRepository.findByUserId(USER_ID)).thenReturn(roles);
        when(userRoleRepository.existsByUserIdAndRoleCode(USER_ID, RoleCode.USER))
                .thenReturn(true);

        service().removeMembership(99L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserRole>> captor = ArgumentCaptor.forClass(List.class);
        verify(userRoleRepository).deleteAll(captor.capture());

        List<RoleCode> revoked = captor.getValue().stream()
                .map(userRole -> userRole.getRole().getCode())
                .toList();

        assertThat(revoked).containsExactlyInAnyOrder(
                RoleCode.TECHNICIAN, RoleCode.DRIVER);
        assertThat(revoked).doesNotContain(RoleCode.USER);
    }

    /**
     * Being an employee and being a customer are separate identities.
     * Removing the employee membership must neither create nor destroy a
     * customer identity.
     */
    @Test
    void remove_leavesAnExistingCustomerRoleAlone() {

        List<UserRole> roles = new ArrayList<>(List.of(
                userRoleOf(RoleCode.USER),
                userRoleOf(RoleCode.CUSTOMER),
                userRoleOf(RoleCode.MANAGER)
        ));

        when(membershipRepository.findById(99L))
                .thenReturn(Optional.of(membershipWith(GarageMembershipStatus.ACTIVE)));
        when(userRoleRepository.findByUserId(USER_ID)).thenReturn(roles);
        when(userRoleRepository.existsByUserIdAndRoleCode(USER_ID, RoleCode.USER))
                .thenReturn(true);

        service().removeMembership(99L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserRole>> captor = ArgumentCaptor.forClass(List.class);
        verify(userRoleRepository).deleteAll(captor.capture());

        List<RoleCode> revoked = captor.getValue().stream()
                .map(userRole -> userRole.getRole().getCode())
                .toList();

        assertThat(revoked).containsExactly(RoleCode.MANAGER);
        assertThat(revoked).doesNotContain(RoleCode.CUSTOMER);
    }

    @Test
    void remove_restoresTheBaseUserRoleIfAPreviousRemovalWipedIt() {

        Role userRole = new Role();
        userRole.setCode(RoleCode.USER);

        when(membershipRepository.findById(99L))
                .thenReturn(Optional.of(membershipWith(GarageMembershipStatus.ACTIVE)));
        when(userRoleRepository.findByUserId(USER_ID))
                .thenReturn(new ArrayList<>(List.of(userRoleOf(RoleCode.TECHNICIAN))));
        when(userRoleRepository.existsByUserIdAndRoleCode(USER_ID, RoleCode.USER))
                .thenReturn(false);
        when(roleRepository.findByCode(RoleCode.USER)).thenReturn(Optional.of(userRole));

        service().removeMembership(99L);

        ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(captor.capture());

        assertThat(captor.getValue().getRole().getCode()).isEqualTo(RoleCode.USER);
    }

    @Test
    void remove_detachesTheUserFromTheGarageAndMarksTheMembershipRemoved() {

        GarageMembership membership = membershipWith(GarageMembershipStatus.ACTIVE);
        user.setGarageId(GARAGE_ID);
        user.setEmployeeCode("G010-EMP001");

        when(membershipRepository.findById(99L)).thenReturn(Optional.of(membership));
        when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(userRoleRepository.existsByUserIdAndRoleCode(USER_ID, RoleCode.USER))
                .thenReturn(true);

        service().removeMembership(99L);

        assertThat(user.getGarageId()).isNull();
        assertThat(user.getEmployeeCode()).isNull();
        assertThat(membership.getStatus()).isEqualTo(GarageMembershipStatus.REMOVED);
    }

    // ---- The full round trip the defect report describes ----

    @Test
    void applyApproveRemoveReapply_endsBackAtPending() {

        // 1. apply
        when(membershipRepository.findByGarage_IdAndUser_Id(GARAGE_ID, USER_ID))
                .thenReturn(Optional.empty());
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<GarageMembership> created =
                ArgumentCaptor.forClass(GarageMembership.class);
        service().joinGarage(USER_ID, joinRequest());
        verify(membershipRepository).save(created.capture());

        GarageMembership membership = created.getValue();
        membership.setId(99L);
        assertThat(membership.getStatus()).isEqualTo(GarageMembershipStatus.PENDING);

        // 2. approved (state the owner's approval would leave behind)
        membership.setStatus(GarageMembershipStatus.ACTIVE);
        membership.setEmployeeCode("G010-EMP001");

        // 3. removed
        when(membershipRepository.findById(99L)).thenReturn(Optional.of(membership));
        when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(userRoleRepository.existsByUserIdAndRoleCode(USER_ID, RoleCode.USER))
                .thenReturn(true);

        service().removeMembership(99L);
        assertThat(membership.getStatus()).isEqualTo(GarageMembershipStatus.REMOVED);

        // 4. re-apply - this is what used to fail
        when(membershipRepository.findByGarage_IdAndUser_Id(GARAGE_ID, USER_ID))
                .thenReturn(Optional.of(membership));

        service().joinGarage(USER_ID, joinRequest());

        assertThat(membership.getStatus()).isEqualTo(GarageMembershipStatus.PENDING);
        assertThat(membership.getEmployeeCode()).isNull();
    }
}
