package com.garageos.modules.catalog.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CatalogItemResponse {

    private Long id;
    private String itemType;
    private String name;
    private String description;
    private String defaultUnit;
    private String hsnSacCode;
    private String classificationStatus;
    private String taxRuleCode;
    private String category;
    private String classificationSource;
    private java.time.LocalDateTime classificationVerifiedAt;
    private Long version;
    /** Rate in force today for the tax rule, or null when unclassified. Informational only; lines snapshot their own rate. */
    private BigDecimal currentRatePercent;
    private boolean active;
}
