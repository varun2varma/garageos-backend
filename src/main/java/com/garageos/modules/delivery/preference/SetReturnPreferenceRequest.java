package com.garageos.modules.delivery.preference;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SetReturnPreferenceRequest {

    @NotNull
    private ReturnMethod method;

    /** Required for DRIVER_DELIVERY; ignored for COLLECT_AT_GARAGE. */
    private String deliveryAddress;
    private BigDecimal deliveryLatitude;
    private BigDecimal deliveryLongitude;
}
