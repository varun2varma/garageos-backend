package com.garageos.modules.navigation.dto.response;

import com.garageos.core.enums.navigation.TripLeg;
import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.core.enums.navigation.TripType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
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

    /**
     * The canonical coordinates for this trip's destination, resolved
     * from the linked NavigationRequest rather than stored a second time
     * here - one source of truth, no duplication, no migration.
     *
     * For a PICKUP trip these are the customer's pickup coordinates; for
     * a DELIVERY trip they are the delivery coordinates. Null when the
     * booking predates coordinate capture (V41), in which case the driver
     * has only the address - which the client must present honestly
     * rather than pretending it has a location.
     */
    private BigDecimal destinationLatitude;
    private BigDecimal destinationLongitude;

    /** Customer id behind this trip, so the driver need not search. */
    private Long customerId;

    private LocalDateTime acceptedAt;

    private LocalDateTime startedAt;

    private LocalDateTime arrivedAt;

    private LocalDateTime completedAt;
}