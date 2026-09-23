package com.garageos.modules.navigation.dto.response;

import com.garageos.core.enums.navigation.TripLeg;
import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.core.enums.navigation.TripType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row of the manager fleet map (Mission Part O/P) - a trip plus the
 * driver identity and last-known-location fields the map needs, so the
 * Flutter fleet screen does not have to make a second per-trip call for
 * either. Deliberately a distinct DTO from NavigationTripResponse rather
 * than adding these fields there: those are driver/customer-facing fields
 * with narrower audiences (see NavigationTripServiceImpl.authorizeViewer),
 * this is manager/owner-facing and always includes driver name + last
 * position for every trip in the list.
 */
@Data
@Builder
public class FleetTripResponse {

    private Long id;

    private TripType tripType;

    private TripLeg currentLeg;

    private TripStatus status;

    private Long driverId;

    private String driverName;

    private Long vehicleId;

    private String destinationAddress;

    private BigDecimal destinationLatitude;
    private BigDecimal destinationLongitude;

    /** Null until the driver's first GPS report for this trip. */
    private Double currentLatitude;
    private Double currentLongitude;
    private LocalDateTime lastLocationUpdate;

    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime arrivedAt;
}
