package com.garageos.modules.navigation.dto.request;

import com.garageos.core.enums.navigation.NavigationRequestType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateNavigationRequest {

    @NotNull
    private Long vehicleId;

    @NotNull
    private Long garageId;

    @NotNull
    private NavigationRequestType requestType;

    private String pickupAddress;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private BigDecimal pickupLatitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private BigDecimal pickupLongitude;

    private String deliveryAddress;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private BigDecimal deliveryLatitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private BigDecimal deliveryLongitude;

    @NotNull
    @Future
    private LocalDateTime scheduledAt;
}