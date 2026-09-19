package com.garageos.modules.owner.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class OwnerDashboardSummaryResponse {

    /*
     * Selected reporting period.
     */
    private LocalDate from;
    private LocalDate to;

    /*
     * Period metrics.
     */
    private Long jobsCreated;
    private Long completedJobs;
    private BigDecimal revenue;

    /*
     * Current operational snapshot.
     *
     * These are NOT restricted by the selected date range.
     * They represent the current state of the garages.
     */
    private Long activeJobs;
    private Long pendingEstimates;
    private Long readyForDelivery;

    private Long inspectionJobs;
    private Long estimateJobs;
    private Long repairJobs;
    private Long qualityCheckJobs;
    private Long readyForInvoiceJobs;
    private Long paymentPendingJobs;

    /*
     * Lifetime garage-scoped totals.
     */
    private Long totalCustomers;
    private Long totalVehicles;
    private Long totalJobCards;
    private Long totalInvoices;

    /*
     * Jobs created during the selected period.
     */
    private List<OwnerDashboardRecentJobResponse> recentJobs;
}