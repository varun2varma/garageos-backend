package com.garageos.modules.garage.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Customer-facing discovery result. Deliberately minimal: no PAN, GSTIN,
 * owner or membership data - only what is needed to choose a garage.
 */
@Getter
@Builder
public class NearbyGarageResponse {

    private Long id;

    private String garageName;

    private String address;

    private String landmark;

    private String city;

    private String state;

    private String pincode;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Boolean hasLogo;

    /** Great-circle distance from the supplied coordinates, rounded to 0.1 km. */
    private double distanceKm;
}
