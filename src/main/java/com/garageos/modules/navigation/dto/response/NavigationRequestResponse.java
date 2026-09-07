package com.garageos.modules.navigation.dto.response;

import com.garageos.core.enums.navigation.NavigationRequestStatus;
import com.garageos.core.enums.navigation.NavigationRequestType;
import lombok.Builder;
import lombok.Data;

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

    private String deliveryAddress;

    private LocalDateTime scheduledAt;

    private NavigationRequestStatus status;

    private LocalDateTime createdAt;
}