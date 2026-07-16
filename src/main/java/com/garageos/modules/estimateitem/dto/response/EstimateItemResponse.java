package com.garageos.modules.estimateitem.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class EstimateItemResponse {

    private Long id;

    private Long estimateId;

    private Long complaintId;

    private String itemType;

    private String description;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

}