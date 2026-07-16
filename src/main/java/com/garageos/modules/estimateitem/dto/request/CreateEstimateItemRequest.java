package com.garageos.modules.estimateitem.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateEstimateItemRequest {

    private Long complaintId;

    private String itemType;

    private String description;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

}