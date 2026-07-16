package com.garageos.modules.estimate.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class EstimateResponse {

    private Long id;

    private String estimateNumber;

    private Long jobCardId;

    private String status;

    private BigDecimal subtotal;

    private BigDecimal discount;

    private BigDecimal gst;

    private BigDecimal grandTotal;

    private String remarks;

}