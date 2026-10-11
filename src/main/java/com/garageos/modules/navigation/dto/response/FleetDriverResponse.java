package com.garageos.modules.navigation.dto.response;

import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.core.enums.navigation.TripType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * One driver of the garage on the fleet map, whether or not the driver has a trip. Everything here is read from real
 * records: the driver's account, the active trip if any, and the last GPS position the driver ever reported.
 *
 * <ul>
 *   <li>{@code availability}: {@code ON_TRIP} when the driver has an active trip (assigned, accepted or in progress),
 *       otherwise {@code AVAILABLE}. The system has no separate "off duty" flag; only ACTIVE driver accounts are listed.</li>
 *   <li>{@code locationState}: {@code LIVE} (position newer than the stale threshold), {@code STALE} (older), or
 *       {@code NONE} (the driver has never reported a position). A position is never invented and never borrowed from
 *       another driver; {@code latitude}/{@code longitude} are null when {@code locationState} is {@code NONE}.</li>
 *   <li>A driver is only ever reported by the server while on a trip, so an AVAILABLE driver's position is the last
 *       known position from their previous trip, flagged by {@code locationState} and {@code lastLocationUpdate}.</li>
 * </ul>
 */
@Data
@Builder
public class FleetDriverResponse {

    public enum Availability { AVAILABLE, ON_TRIP }

    public enum LocationState { LIVE, STALE, NONE }

    private Long driverId;
    private String driverName;
    /** For the call button; visible only to operational staff of the same garage. */
    private String mobile;
    private Availability availability;
    private LocationState locationState;
    private Double latitude;
    private Double longitude;
    private Double heading;
    private LocalDateTime lastLocationUpdate;
    /** Seconds between the last position and the moment the server answered (null when there is no position). */
    private Long locationAgeSeconds;
    private Long activeTripId;
    private TripType activeTripType;
    private TripStatus activeTripStatus;
}
