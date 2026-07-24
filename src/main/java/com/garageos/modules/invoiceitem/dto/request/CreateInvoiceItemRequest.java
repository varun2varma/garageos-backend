package com.garageos.modules.invoiceitem.dto.request;

import com.garageos.core.enums.EstimateItemType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceItemRequest {

    private Long complaintId;

    @NotNull(message = "Item type is required.")
    private EstimateItemType itemType;

    @NotBlank(message = "Description is required.")
    private String description;

    @NotNull(message = "Quantity is required.")
    @DecimalMin("0.01")
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required.")
    @DecimalMin(value = "0.00")
    private BigDecimal unitPrice;

}