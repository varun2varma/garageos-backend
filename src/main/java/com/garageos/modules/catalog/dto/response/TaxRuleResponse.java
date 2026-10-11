package com.garageos.modules.catalog.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class TaxRuleResponse {

    private Long id;
    private String ruleCode;
    private String description;
    private BigDecimal ratePercent;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String sourceReference;
    private LocalDate verifiedOn;
    private boolean active;
}
