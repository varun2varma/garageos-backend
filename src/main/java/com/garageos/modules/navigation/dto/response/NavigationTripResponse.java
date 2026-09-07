package com.garageos.modules.navigation.dto.response;

import com.garageos.core.enums.navigation.TripLeg;
import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.core.enums.navigation.TripType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NavigationTripResponse {

    private Long id;

    private Long navigationRequestId;

    private Long vehicleId;

    private Long driverId;

    private TripType tripType;

    private TripLeg currentLeg;

    private TripStatus status;

    private String sourceAddress;

    private String destinationAddress;

    private LocalDateTime acceptedAt;

    private LocalDateTime startedAt;

    private LocalDateTime arrivedAt;

    private LocalDateTime completedAt;
}