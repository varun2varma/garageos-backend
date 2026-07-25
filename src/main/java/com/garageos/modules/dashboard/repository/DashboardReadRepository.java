package com.garageos.modules.dashboard.repository;

import com.garageos.modules.dashboard.dto.response.DashboardSummaryResponse;
import com.garageos.modules.dashboard.dto.response.RecentJobResponse;

import java.util.List;

public interface DashboardReadRepository {

    DashboardSummaryResponse getDashboardSummary();

    List<RecentJobResponse> getRecentJobs();

}