package com.garageos.modules.dashboard.service.impl;

import com.garageos.core.enums.garagemembership.GarageMembershipStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.dashboard.dto.response.DashboardSummaryResponse;
import com.garageos.modules.dashboard.dto.response.RecentJobResponse;
import com.garageos.modules.dashboard.repository.DashboardReadRepository;
import com.garageos.modules.dashboard.service.DashboardService;
import com.garageos.modules.garagemembership.repository.GarageMembershipRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardReadRepository dashboardReadRepository;

    private final GarageMembershipRepository garageMembershipRepository;

    @Override
    public DashboardSummaryResponse getDashboardSummary(Long requestedGarageId, boolean allMyGarages) {
        return dashboardReadRepository.getDashboardSummary(resolveGarageIds(requestedGarageId, allMyGarages));
    }

    @Override
    public List<RecentJobResponse> getRecentJobs(Long requestedGarageId, boolean allMyGarages) {
        return dashboardReadRepository.getRecentJobs(resolveGarageIds(requestedGarageId, allMyGarages));
    }

    /**
     * Every Manager/Advisor/employee gets their own garage context, always
     * (requestedGarageId/allMyGarages are unused for them in practice). A
     * multi-garage Owner may pass a different garageId to view that garage
     * instead, or allMyGarages=true to aggregate across every garage they
     * belong to - but only ever garages they actually have an ACTIVE
     * membership in; never trusted blindly.
     */
    private List<Long> resolveGarageIds(Long requestedGarageId, boolean allMyGarages) {

        GarageUserPrincipal principal = (GarageUserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (requestedGarageId != null) {

            boolean hasMembership = garageMembershipRepository
                    .existsByGarage_IdAndUser_Id(requestedGarageId, principal.getId());

            if (!hasMembership) {
                throw new ResourceNotFoundException("Garage not found with id : " + requestedGarageId);
            }

            return List.of(requestedGarageId);
        }

        if (allMyGarages) {

            List<Long> myGarageIds = garageMembershipRepository
                    .findByUser_Id(principal.getId())
                    .stream()
                    .filter(m -> m.getStatus() == GarageMembershipStatus.ACTIVE)
                    .map(m -> m.getGarage().getId())
                    .toList();

            if (myGarageIds.isEmpty()) {
                throw new BusinessException("No garage context for this account.");
            }

            return myGarageIds;
        }

        if (principal.getGarageId() == null) {
            throw new BusinessException("No garage context for this account.");
        }

        return List.of(principal.getGarageId());
    }
}
