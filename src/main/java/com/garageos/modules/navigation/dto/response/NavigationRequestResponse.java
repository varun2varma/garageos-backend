package com.garageos.modules.navigation.dto.response;

import com.garageos.core.enums.navigation.NavigationRequestStatus;
import com.garageos.core.enums.navigation.NavigationRequestType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class NavigationRequestResponse {

    private Long id;

    private Long customerId;

    private Long vehicleId;

    private Long garageId;

    private NavigationRequestType requestType;

    private String pickupAddress;

    /**
     * Canonical coordinates carried from the booking. Previously absent,
     * so a driver had only the free-text address to work from.
     */
    private BigDecimal pickupLatitude;
    private BigDecimal pickupLongitude;

    private String deliveryAddress;

    private BigDecimal deliveryLatitude;
    private BigDecimal deliveryLongitude;

    private LocalDateTime scheduledAt;

    private NavigationRequestStatus status;

    private LocalDateTime createdAt;
}