package com.garageos.modules.booking.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateBookingRequest {

    @NotNull
    private Long vehicleId;

    @NotNull
    private Long garageId;

    @NotBlank
    private String serviceDescription;

    private String concerns;

    @NotNull
    @Future
    private LocalDateTime requestedAt;

    private boolean pickupRequested;

    /**
     * Human-readable pickup location. Descriptive metadata only — a
     * driver cannot navigate to a typed place name.
     */
    private String pickupAddress;

    /**
     * Canonical pickup coordinates, required when
     * {@link #pickupRequested} is true — validated in the service layer
     * (matching CreateNavigationRequest's own pattern for conditionally
     * required fields).
     *
     * These are what the pickup trip actually navigates to; the address
     * above is shown alongside them, never used in their place.
     */
    @DecimalMin(value = "-90.0", message = "Pickup latitude must be between -90 and 90.")
    @DecimalMax(value = "90.0", message = "Pickup latitude must be between -90 and 90.")
    private BigDecimal pickupLatitude;

    @DecimalMin(value = "-180.0", message = "Pickup longitude must be between -180 and 180.")
    @DecimalMax(value = "180.0", message = "Pickup longitude must be between -180 and 180.")
    private BigDecimal pickupLongitude;
}
