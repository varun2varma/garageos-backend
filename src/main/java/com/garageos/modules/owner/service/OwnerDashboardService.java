package com.garageos.modules.owner.service;

import com.garageos.modules.owner.dto.response.OwnerDashboardSummaryResponse;

import java.time.LocalDate;

public interface OwnerDashboardService {

    OwnerDashboardSummaryResponse getSummary(
            Long garageId,
            boolean allGarages,
            LocalDate from,
            LocalDate to
    );
}