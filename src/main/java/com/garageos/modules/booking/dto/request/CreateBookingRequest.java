package com.garageos.modules.booking.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

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
     * Required when {@link #pickupRequested} is true — validated in the
     * service layer (matches CreateNavigationRequest's own pattern for the
     * equivalent conditional-required address field).
     */
    private String pickupAddress;
}
