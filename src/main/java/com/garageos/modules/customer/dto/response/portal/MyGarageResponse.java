package com.garageos.modules.customer.dto.response.portal;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** A garage the customer has an established service relationship with ("My Garages"). */
@Getter
@Builder
public class MyGarageResponse {

    private Long garageId;

    private String garageName;

    private String address;

    private String city;

    private String state;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String status;

    /** True when the garage is ACTIVE and can receive a new booking. */
    private boolean bookable;

    /**
     * Why {@link #bookable} is false ({@code GARAGE_INACTIVE}); null when it is bookable. Lets the app say so
     * explicitly instead of silently offering another garage.
     */
    private String unavailableReason;

    @Builder.Default
    private boolean defaultGarage = false;
}
