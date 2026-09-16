package com.garageos.modules.dashboard.service.impl;

import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.enums.garagemembership.GarageMembershipStatus;
import com.garageos.modules.dashboard.dto.response.DashboardSummaryResponse;
import com.garageos.modules.dashboard.repository.DashboardReadRepository;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garagemembership.entity.GarageMembership;
import com.garageos.modules.garagemembership.repository.GarageMembershipRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Corrective fix: DashboardReadRepositoryImpl previously aggregated across
 * ALL garages with zero tenant scoping - every Manager/Advisor/Owner
 * dashboard showed identical system-wide numbers regardless of garage.
 * These tests cover the authorization layer added on top (garage
 * resolution + cross-garage Owner override), not the JPQL scoping itself
 * (which needs a real database - see the environment-dependent
 * GarageOsApplicationTests note in the completion report).
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock private DashboardReadRepository dashboardReadRepository;
    @Mock private GarageMembershipRepository garageMembershipRepository;

    @InjectMocks
    private DashboardServiceImpl service;

    private static final Long GARAGE_ID = 10L;
    private static final Long OTHER_GARAGE_ID = 11L;
    private static final Long USER_ID = 1L;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long garageId) {
        GarageUserPrincipal principal = new GarageUserPrincipal(
                USER_ID, garageId, "owner1", "hash", "First", "Last",
                "owner@example.test", "9999999999", false, UserStatus.ACTIVE,
                Set.of("OWNER"), Set.of(), List.of()
        );
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private GarageMembership activeMembership(Long garageId) {
        Garage garage = new Garage();
        garage.setId(garageId);
        GarageMembership m = new GarageMembership();
        m.setGarage(garage);
        m.setStatus(GarageMembershipStatus.ACTIVE);
        return m;
    }

    @Test
    void noRequestedGarage_usesCallersOwnGarage() {

        authenticate(GARAGE_ID);
        when(dashboardReadRepository.getDashboardSummary(List.of(GARAGE_ID)))
                .thenReturn(DashboardSummaryResponse.builder().build());

        service.getDashboardSummary(null, false);

        verify(dashboardReadRepository).getDashboardSummary(List.of(GARAGE_ID));
    }

    @Test
    void requestedGarage_withActiveMembership_isHonored() {

        authenticate(GARAGE_ID);
        when(garageMembershipRepository.existsByGarage_IdAndUser_Id(OTHER_GARAGE_ID, USER_ID))
                .thenReturn(true);
        when(dashboardReadRepository.getDashboardSummary(List.of(OTHER_GARAGE_ID)))
                .thenReturn(DashboardSummaryResponse.builder().build());

        service.getDashboardSummary(OTHER_GARAGE_ID, false);

        verify(dashboardReadRepository).getDashboardSummary(List.of(OTHER_GARAGE_ID));
    }

    @Test
    void requestedGarage_withoutMembership_isRejected_neverQueried() {

        authenticate(GARAGE_ID);
        when(garageMembershipRepository.existsByGarage_IdAndUser_Id(OTHER_GARAGE_ID, USER_ID))
                .thenReturn(false);

        assertThatThrownBy(() -> service.getDashboardSummary(OTHER_GARAGE_ID, false))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(dashboardReadRepository, never()).getDashboardSummary(any());
    }

    @Test
    void noGarageContextAtAll_isRejected() {

        authenticate(null);

        assertThatThrownBy(() -> service.getDashboardSummary(null, false))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getRecentJobs_appliesSameGarageResolution() {

        authenticate(GARAGE_ID);
        when(dashboardReadRepository.getRecentJobs(List.of(GARAGE_ID))).thenReturn(List.of());

        service.getRecentJobs(null, false);

        verify(dashboardReadRepository).getRecentJobs(List.of(GARAGE_ID));
    }

    @Test
    void allMyGarages_aggregatesAcrossEveryActiveMembership() {

        authenticate(GARAGE_ID);
        when(garageMembershipRepository.findByUser_Id(USER_ID))
                .thenReturn(List.of(activeMembership(GARAGE_ID), activeMembership(OTHER_GARAGE_ID)));
        when(dashboardReadRepository.getDashboardSummary(List.of(GARAGE_ID, OTHER_GARAGE_ID)))
                .thenReturn(DashboardSummaryResponse.builder().build());

        service.getDashboardSummary(null, true);

        verify(dashboardReadRepository).getDashboardSummary(List.of(GARAGE_ID, OTHER_GARAGE_ID));
    }
}
