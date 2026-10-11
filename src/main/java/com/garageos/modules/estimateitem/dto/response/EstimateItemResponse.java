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

    /**
     * The linked Complaint's own text (e.g. "AC not cooling"). Additive:
     * needed so the customer approval UI can group/label items by
     * Complaint - the actual customer approval boundary - rather than by
     * a bare numeric complaintId.
     */
    private String complaintText;

    private String itemType;

    private String description;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private Boolean selected;

    private Long catalogItemId;

    private String unit;

    /** LEGACY, MASTER or MANUAL. */
    private String entrySource;

    /** NOT_REQUIRED, NEEDS_REVIEW or RESOLVED. */
    private String reviewStatus;

    private String manualNote;

    private String hsnSacCode;

    private BigDecimal gstRatePercent;
}