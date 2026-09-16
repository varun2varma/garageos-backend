package com.garageos.modules.garagemembership.service.impl;

import com.garageos.core.enums.garagemembership.GarageMembershipStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.garagemembership.dto.request.ApproveGarageMembershipRequest;
import com.garageos.modules.garagemembership.entity.GarageMembership;
import com.garageos.modules.garagemembership.mapper.GarageMembershipMapper;
import com.garageos.modules.garagemembership.repository.GarageMembershipRepository;
import com.garageos.modules.identity.entity.Role;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.RoleRepository;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers the concurrency-safe replacement for the previous
 * MAX(existing employee_code) + 1 scan: employee codes now come from
 * Garage.nextEmployeeSequence, read-and-incremented via
 * GarageRepository.findByIdForUpdate() (a pessimistic row lock) inside
 * this service's class-level @Transactional. A pure Mockito test can't
 * exercise a real concurrent race (mocks aren't a real transactional
 * datastore) - it proves the service asks for the lock and increments
 * the counter it was given, not a max-scan. Genuine concurrent-safety
 * (two simultaneous approvals never producing the same code) and the
 * DB unique constraint were verified live against Postgres - see the
 * E2E verification report.
 */
@ExtendWith(MockitoExtension.class)
class GarageMembershipServiceImplTest {

    @Mock private GarageMembershipRepository membershipRepository;
    @Mock private GarageRepository garageRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private GarageMembershipMapper mapper;

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

    private Garage garage(Long id, String code, int nextSequence) {
        Garage g = new Garage();
        g.setId(id);
        g.setGarageCode(code);
        g.setNextEmployeeSequence(nextSequence);
        return g;
    }

    private GarageMembership pendingMembership(Long id, Garage garage, User employee) {
        GarageMembership m = new GarageMembership();
        m.setId(id);
        m.setGarage(garage);
        m.setUser(employee);
        m.setStatus(GarageMembershipStatus.PENDING);
        return m;
    }

    private ApproveGarageMembershipRequest approveRequest(Long roleId) {
        ApproveGarageMembershipRequest request = new ApproveGarageMembershipRequest();
        request.setRoleIds(List.of(roleId));
        return request;
    }

    private void stubCommonApprovalDependencies(User owner, Role role) {
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));
        when(userRoleRepository.existsByUserIdAndRoleCode(any(), any())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any()))
                .thenAnswer(inv -> new com.garageos.modules.garagemembership.dto.response.GarageMembershipResponse());
        when(userRoleRepository.findByUserId(any())).thenReturn(List.of());
    }

    @Test
    void approveMembership_firstEmployeeInGarage_getsSequenceOne() {

        Garage garageA = garage(6L, "G006", 1);
        User owner = new User(); owner.setId(100L);
        User employee = new User(); employee.setId(200L);
        Role role = new Role(); role.setId(3L); role.setCode(RoleCode.MANAGER);

        GarageMembership membership = pendingMembership(1L, garageA, employee);
        when(membershipRepository.findById(1L)).thenReturn(Optional.of(membership));
        when(garageRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(garageA));
        stubCommonApprovalDependencies(owner, role);

        service().approveMembership(1L, 100L, approveRequest(3L));

        assertThat(employee.getEmployeeCode()).isEqualTo("G006-EMP001");
        assertThat(membership.getEmployeeCode()).isEqualTo("G006-EMP001");
        assertThat(garageA.getNextEmployeeSequence()).isEqualTo(2);
    }

    @Test
    void approveMembership_secondEmployeeInSameGarage_getsSequenceTwo() {

        // Garage already advanced to 2 by a prior approval - this is the
        // state findByIdForUpdate would return for the second call.
        Garage garageA = garage(6L, "G006", 2);
        User owner = new User(); owner.setId(100L);
        User employee = new User(); employee.setId(201L);
        Role role = new Role(); role.setId(5L); role.setCode(RoleCode.TECHNICIAN);

        GarageMembership membership = pendingMembership(2L, garageA, employee);
        when(membershipRepository.findById(2L)).thenReturn(Optional.of(membership));
        when(garageRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(garageA));
        stubCommonApprovalDependencies(owner, role);

        service().approveMembership(2L, 100L, approveRequest(5L));

        assertThat(employee.getEmployeeCode()).isEqualTo("G006-EMP002");
        assertThat(garageA.getNextEmployeeSequence()).isEqualTo(3);
    }

    @Test
    void approveMembership_differentGarage_sequencesIndependently() {

        Garage garageB = garage(7L, "G007", 1);
        User owner = new User(); owner.setId(101L);
        User employee = new User(); employee.setId(300L);
        Role role = new Role(); role.setId(3L); role.setCode(RoleCode.MANAGER);

        GarageMembership membership = pendingMembership(3L, garageB, employee);
        when(membershipRepository.findById(3L)).thenReturn(Optional.of(membership));
        when(garageRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(garageB));
        stubCommonApprovalDependencies(owner, role);

        service().approveMembership(3L, 101L, approveRequest(3L));

        assertThat(employee.getEmployeeCode()).isEqualTo("G007-EMP001");
    }

    @Test
    void approveMembership_usesLockedFetch_notAFullTableScan() {

        Garage garageA = garage(6L, "G006", 1);
        User owner = new User(); owner.setId(100L);
        User employee = new User(); employee.setId(200L);
        Role role = new Role(); role.setId(3L); role.setCode(RoleCode.MANAGER);

        GarageMembership membership = pendingMembership(1L, garageA, employee);
        when(membershipRepository.findById(1L)).thenReturn(Optional.of(membership));
        when(garageRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(garageA));
        stubCommonApprovalDependencies(owner, role);

        service().approveMembership(1L, 100L, approveRequest(3L));

        verify(garageRepository).findByIdForUpdate(6L);
        verify(membershipRepository, never()).findByGarage_Id(any());
    }
}
