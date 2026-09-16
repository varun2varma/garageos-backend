package com.garageos.modules.garagemembership.service.impl;

import com.garageos.core.enums.garagemembership.GarageMembershipStatus;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.garagemembership.dto.response.GarageMembershipResponse;
import com.garageos.modules.garagemembership.entity.GarageMembership;
import com.garageos.modules.garagemembership.mapper.GarageMembershipMapper;
import com.garageos.modules.garagemembership.repository.GarageMembershipRepository;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.RoleRepository;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * The Owner garage-switcher's data source: every garage the current user
 * has an ACTIVE membership in (an Owner may belong to several), used to
 * populate the switcher's options. PENDING/REJECTED/REMOVED memberships
 * must never appear here - only ACTIVE ones.
 */
@ExtendWith(MockitoExtension.class)
class GarageMembershipServiceImplMyMembershipsTest {

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

    private GarageMembership membership(Long garageId, GarageMembershipStatus status) {
        User user = new User();
        user.setId(1L);
        user.setFirstName("Ravi");

        Garage garage = new Garage();
        garage.setId(garageId);
        garage.setGarageCode("G0" + garageId);

        GarageMembership m = new GarageMembership();
        m.setUser(user);
        m.setGarage(garage);
        m.setStatus(status);
        return m;
    }

    @Test
    void returnsOnlyActiveMemberships_acrossMultipleGarages() {

        GarageMembership activeA = membership(10L, GarageMembershipStatus.ACTIVE);
        GarageMembership activeB = membership(11L, GarageMembershipStatus.ACTIVE);
        GarageMembership pending = membership(12L, GarageMembershipStatus.PENDING);

        when(membershipRepository.findByUser_Id(1L)).thenReturn(List.of(activeA, activeB, pending));
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of());
        when(mapper.toResponse(activeA)).thenReturn(responseFor(10L));
        when(mapper.toResponse(activeB)).thenReturn(responseFor(11L));

        List<GarageMembershipResponse> result = service().getMyMemberships(1L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(GarageMembershipResponse::getGarageId).containsExactlyInAnyOrder(10L, 11L);
    }

    private GarageMembershipResponse responseFor(Long garageId) {
        GarageMembershipResponse r = new GarageMembershipResponse();
        r.setGarageId(garageId);
        return r;
    }
}
