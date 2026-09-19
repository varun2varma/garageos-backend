package com.garageos.modules.owner.service.impl;

import com.garageos.core.enums.garagemembership.GarageMembershipStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.garagemembership.entity.GarageMembership;
import com.garageos.modules.garagemembership.repository.GarageMembershipRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.owner.dto.response.OwnerDashboardSummaryResponse;
import com.garageos.modules.owner.repository.OwnerDashboardReadRepository;
import com.garageos.modules.owner.service.OwnerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OwnerDashboardServiceImpl implements OwnerDashboardService {

    private final OwnerDashboardReadRepository ownerDashboardReadRepository;

    private final GarageMembershipRepository garageMembershipRepository;

    @Override
    @Transactional(readOnly = true)
    public OwnerDashboardSummaryResponse getSummary(
            Long requestedGarageId,
            boolean allGarages,
            LocalDate from,
            LocalDate to
    ) {

        GarageUserPrincipal principal = getPrincipal();

        validateOwner(principal);

        LocalDate effectiveFrom =
                from != null ? from : LocalDate.now();

        LocalDate effectiveTo =
                to != null ? to : effectiveFrom;

        if (effectiveTo.isBefore(effectiveFrom)) {
            throw new BusinessException(
                    "Dashboard date range is invalid: 'to' cannot be before 'from'."
            );
        }

        List<Long> garageIds =
                resolveGarageIds(
                        principal,
                        requestedGarageId,
                        allGarages
                );

        return ownerDashboardReadRepository.getSummary(
                garageIds,
                effectiveFrom,
                effectiveTo
        );
    }

    private GarageUserPrincipal getPrincipal() {

        Object principal =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        if (!(principal instanceof GarageUserPrincipal garageUserPrincipal)) {
            throw new BusinessException(
                    "Authenticated garage user context is required."
            );
        }

        return garageUserPrincipal;
    }

    private void validateOwner(GarageUserPrincipal principal) {

        if (principal.getRoles() == null
                || !principal.getRoles()
                .contains(RoleCode.OWNER.name())) {

            throw new BusinessException(
                    "Only garage owners can access the owner dashboard."
            );
        }
    }

    private List<Long> resolveGarageIds(
            GarageUserPrincipal principal,
            Long requestedGarageId,
            boolean allGarages
    ) {

        /*
         * Explicit garage selection.
         */
        if (requestedGarageId != null) {

            boolean hasMembership =
                    garageMembershipRepository
                            .existsByGarage_IdAndUser_Id(
                                    requestedGarageId,
                                    principal.getId()
                            );

            if (!hasMembership) {
                throw new ResourceNotFoundException(
                        "Garage not found for this owner."
                );
            }

            return List.of(requestedGarageId);
        }

        /*
         * Owner explicitly requested all garages.
         */
        if (allGarages) {

            List<Long> garageIds =
                    garageMembershipRepository
                            .findByUser_Id(principal.getId())
                            .stream()
                            .filter(this::isActiveMembership)
                            .map(membership ->
                                    membership.getGarage().getId()
                            )
                            .toList();

            if (garageIds.isEmpty()) {
                throw new BusinessException(
                        "No active garages found for this owner."
                );
            }

            return garageIds;
        }

        /*
         * No explicit garage selection.
         *
         * Prefer the currently authenticated garage context.
         */
        if (principal.getGarageId() != null) {
            return List.of(principal.getGarageId());
        }

        /*
         * If there is no current garage context, fall back to
         * all active garages rather than returning an ambiguous
         * empty dashboard.
         */
        List<Long> garageIds =
                garageMembershipRepository
                        .findByUser_Id(principal.getId())
                        .stream()
                        .filter(this::isActiveMembership)
                        .map(membership ->
                                membership.getGarage().getId()
                        )
                        .toList();

        if (garageIds.isEmpty()) {
            throw new BusinessException(
                    "No active garages found for this owner."
            );
        }

        return garageIds;
    }

    private boolean isActiveMembership(
            GarageMembership membership
    ) {

        return membership.getStatus()
                == GarageMembershipStatus.ACTIVE;
    }
}