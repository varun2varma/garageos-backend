package com.garageos.modules.delivery.preference;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class SetReturnPreferenceRequest {

    @NotNull
    private ReturnMethod method;

    /** Required for DRIVER_DELIVERY; ignored for COLLECT_AT_GARAGE. */
    private String deliveryAddress;
    private BigDecimal deliveryLatitude;
    private BigDecimal deliveryLongitude;

    /** Optional wall-clock time the customer wants the vehicle delivered (DRIVER_DELIVERY only); null = as soon as possible. */
    private LocalDateTime requestedDeliveryAt;
}
