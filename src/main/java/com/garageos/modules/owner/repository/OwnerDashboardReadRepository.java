package com.garageos.modules.owner.repository;

import com.garageos.modules.owner.dto.response.OwnerDashboardRecentJobResponse;
import com.garageos.modules.owner.dto.response.OwnerDashboardSummaryResponse;

import java.time.LocalDate;
import java.util.List;

public interface OwnerDashboardReadRepository {

    OwnerDashboardSummaryResponse getSummary(
            List<Long> garageIds,
            LocalDate from,
            LocalDate to
    );
}