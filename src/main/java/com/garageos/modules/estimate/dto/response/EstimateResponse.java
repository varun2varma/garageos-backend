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

    /** Human-readable business number of the job card (e.g. G001-JC-2026-000001); jobCardId stays the API key. */
    private String jobCardNumber;

    private String status;

    private BigDecimal subtotal;

    private BigDecimal discount;

    private BigDecimal gst;

    private BigDecimal grandTotal;

    private String remarks;

}