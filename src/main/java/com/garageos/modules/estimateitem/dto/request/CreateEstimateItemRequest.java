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

    /** Optional: id of an approved catalog item. Absent means a manual entry. */
    private Long catalogItemId;

    /** Optional unit of measure (for example NOS, LTR, HR). */
    private String unit;

    /** Optional context for a manual entry, used later when the item is reviewed. */
    private String manualNote;

}