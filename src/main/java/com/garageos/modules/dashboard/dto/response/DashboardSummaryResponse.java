package com.garageos.modules.dashboard.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardSummaryResponse {

    Long activeJobs;

    Long pendingEstimates;

    Long readyForDelivery;

    Long completedToday;

    BigDecimal todayRevenue;

    Long inspectionJobs;

    Long estimateJobs;

    Long repairJobs;

    Long qualityCheckJobs;

    Long readyForInvoiceJobs;

    Long paymentPendingJobs;

    Long totalCustomers;

    Long totalVehicles;

    Long totalJobCards;

    Long totalInvoices;

}