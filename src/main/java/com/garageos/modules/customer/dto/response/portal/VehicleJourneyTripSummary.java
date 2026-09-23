package com.garageos.modules.customer.dto.response.portal;

import com.garageos.core.enums.navigation.TripLeg;
import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.core.enums.navigation.TripType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Deliberately lean subset of NavigationTrip for the active vehicle
 * journey card. No GPS coordinates, no ETA/distance and no driver
 * identity here on purpose - live location, routing and the map itself
 * stay owned entirely by the existing NavigationTrip/LocationSocketService
 * infrastructure (see ActiveTripScreen on the Flutter side), reached via
 * {@link #id} when {@code journeyStage} is navigation-related.
 */
@Data
@Builder
public class VehicleJourneyTripSummary {

    private Long id;

    private TripStatus status;

    private TripType tripType;

    private TripLeg currentLeg;

    private LocalDateTime arrivedAt;

    /**
     * Whether a PENDING handover confirmation code currently exists for
     * this trip - lets the card offer "Complete Handover" without the
     * client needing to separately poll the handover endpoints just to
     * find that out.
     */
    private boolean handoverPending;
}
