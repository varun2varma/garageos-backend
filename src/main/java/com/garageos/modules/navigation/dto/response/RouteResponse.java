package com.garageos.modules.navigation.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * A calculated road route - mirrors the Flutter client's own RouteResult
 * shape (core/maps/route_result.dart) deliberately, so the client's
 * existing polyline decoder (core/maps/polyline_codec.dart) keeps working
 * unchanged: encodedPolyline is the same precision-5 encoded-polyline
 * format both Google Routes and OSRM already return, passed through
 * as-is rather than expanded into a raw point array.
 */
@Data
@Builder
public class RouteResponse {

    private Double distanceMeters;

    private Double durationSeconds;

    private String encodedPolyline;

    /** "GOOGLE" or "OSRM" - which provider actually answered; not used
     * for client logic, only observability/debugging. */
    private String provider;
}
