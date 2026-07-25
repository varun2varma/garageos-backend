package com.garageos.modules.dashboard.service.impl;

import com.garageos.modules.dashboard.dto.response.DashboardSummaryResponse;
import com.garageos.modules.dashboard.dto.response.RecentJobResponse;
import com.garageos.modules.dashboard.repository.DashboardReadRepository;
import com.garageos.modules.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardReadRepository dashboardReadRepository;

    @Override
    public DashboardSummaryResponse getDashboardSummary() {
        return dashboardReadRepository.getDashboardSummary();
    }

    @Override
    public List<RecentJobResponse> getRecentJobs() {
        return dashboardReadRepository.getRecentJobs();
    }
}