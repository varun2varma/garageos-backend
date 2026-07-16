package com.garageos.modules.invoice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateInvoiceRequest {

    @NotNull(message = "Estimate Id is required.")
    private Long estimateId;

    private String remarks;

}