package com.garageos.modules.dashboard.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.modules.dashboard.dto.response.DashboardSummaryResponse;
import com.garageos.modules.dashboard.dto.response.RecentJobResponse;
import com.garageos.modules.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * garageId and allGarages are optional and, when supplied, only
     * honored for a caller with ACTIVE membership(s) there (a multi-garage
     * Owner viewing a specific other garage, or aggregating across all
     * their garages) - see DashboardServiceImpl.resolveGarageIds. Every
     * other caller gets their own garage's numbers exactly as before these
     * parameters existed.
     */
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> getDashboardSummary(
            @RequestParam(required = false) Long garageId,
            @RequestParam(defaultValue = "false") boolean allGarages) {

        DashboardSummaryResponse response =
                dashboardService.getDashboardSummary(garageId, allGarages);

        return ApiResponse.<DashboardSummaryResponse>builder()
                .success(true)
                .message("Dashboard summary fetched successfully.")
                .data(response)
                .timestamp(LocalDateTime.now())
                .requestId(UUID.randomUUID().toString())
                .build();
    }

    @GetMapping("/recent-jobs")
    public ApiResponse<List<RecentJobResponse>> getRecentJobs(
            @RequestParam(required = false) Long garageId,
            @RequestParam(defaultValue = "false") boolean allGarages) {

        List<RecentJobResponse> response =
                dashboardService.getRecentJobs(garageId, allGarages);

        return ApiResponse.<List<RecentJobResponse>>builder()
                .success(true)
                .message("Recent jobs fetched successfully.")
                .data(response)
                .timestamp(LocalDateTime.now())
                .requestId(UUID.randomUUID().toString())
                .build();
    }
}