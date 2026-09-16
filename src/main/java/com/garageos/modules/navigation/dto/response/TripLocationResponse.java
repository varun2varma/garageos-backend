package com.garageos.modules.navigation.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Authorized read of a trip's last known driver location
 * (GET /api/v1/navigation/trips/{tripId}/location). This is the smallest
 * correct REST addition to the existing WebSocket-only location
 * infrastructure - a real position last reported over
 * /app/location/update, never a fabricated one.
 */
@Getter
@Builder
public class TripLocationResponse {

    private Long tripId;
    private Double latitude;
    private Double longitude;
    private Double speed;
    private Double heading;
    private Double accuracy;
    private LocalDateTime lastUpdated;
}
