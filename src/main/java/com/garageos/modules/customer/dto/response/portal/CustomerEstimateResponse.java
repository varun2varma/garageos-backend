package com.garageos.modules.customer.dto.response.portal;

import com.garageos.core.enums.EstimateStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CustomerEstimateResponse {

    private Long id;

    private String estimateNumber;

    private String jobCardNumber;

    private EstimateStatus status;

    private BigDecimal grandTotal;

}