package com.garageos.modules.catalog.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A manually entered estimate line awaiting catalog review. */
@Getter
@Builder
public class ReviewItemResponse {

    private Long id;
    private Long estimateId;
    private Long garageId;
    private String itemType;
    private String description;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;
    private String manualNote;
    private Long createdByUserId;
    private LocalDateTime createdAt;
    private String reviewStatus;
    private Long catalogItemId;
}
