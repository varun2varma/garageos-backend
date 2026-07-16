package com.garageos.modules.delivery.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDeliveryRequest {

    @NotNull(message = "Job Card Id is required.")
    private Long jobCardId;

    @NotNull(message = "Invoice Id is required.")
    private Long invoiceId;

    private String deliveredBy;

    private String receivedBy;

    private String remarks;

}