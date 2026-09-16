package com.garageos.modules.dashboard.service;

import com.garageos.modules.dashboard.dto.response.DashboardSummaryResponse;
import com.garageos.modules.dashboard.dto.response.RecentJobResponse;

import java.util.List;

public interface DashboardService {

    /**
     * @param requestedGarageId null uses the caller's own garage context;
     *                          non-null lets a user with an ACTIVE
     *                          membership in another garage (e.g. a
     *                          multi-garage Owner) view that garage's
     *                          numbers instead - validated against
     *                          GarageMembership, never trusted blindly.
     * @param allMyGarages      when true (and requestedGarageId is null),
     *                          aggregates across every garage the caller
     *                          has an ACTIVE membership in - Owner
     *                          "All Garages". Ignored for a single-garage
     *                          caller.
     */
    DashboardSummaryResponse getDashboardSummary(Long requestedGarageId, boolean allMyGarages);

    List<RecentJobResponse> getRecentJobs(Long requestedGarageId, boolean allMyGarages);

}