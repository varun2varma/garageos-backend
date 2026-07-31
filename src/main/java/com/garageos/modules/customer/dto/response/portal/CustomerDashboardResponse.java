package com.garageos.modules.customer.dto.response.portal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerDashboardResponse {

    private Long vehicleCount;

    private Long activeJobCount;

    private Long pendingEstimateCount;

    private Long pendingInvoiceCount;

}