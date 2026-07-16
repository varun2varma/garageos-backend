package com.garageos.modules.delivery.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeliveryResponse {

    private Long id;

    private Long jobCardId;

    private String jobCardNumber;

    private Long invoiceId;

    private String invoiceNumber;

    private LocalDateTime deliveryDateTime;

    private String deliveredBy;

    private String receivedBy;

    private String status;

    private String remarks;

}