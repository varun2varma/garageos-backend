package com.garageos.modules.invoiceitem.dto.response;

import com.garageos.core.enums.EstimateItemType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemResponse {

    private Long id;

    private Long invoiceId;

    private Long complaintId;

    private String complaintTitle;

    private EstimateItemType itemType;

    private String description;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

}