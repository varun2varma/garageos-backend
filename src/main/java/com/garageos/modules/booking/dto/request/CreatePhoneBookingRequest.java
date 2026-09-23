package com.garageos.modules.booking.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mission backlog #6 — phone-call booking. An employee creates this on
 * behalf of a customer who called in. Deliberately does NOT create a
 * customer: [customerMobile] must match an existing Customer (see
 * BookingServiceImpl.createPhoneBooking) - "do not create duplicate
 * customers unnecessarily" is satisfied by simply never creating one
 * here, reusing the existing employee-side customer-creation flow
 * (features/customer/) for a genuinely new customer first.
 */
@Data
public class CreatePhoneBookingRequest {

    @NotBlank(message = "Customer mobile number is required.")
    private String customerMobile;

    @NotNull(message = "Vehicle is required.")
    private Long vehicleId;

    @NotBlank(message = "Service description is required.")
    private String serviceDescription;

    private String concerns;

    @NotNull(message = "Requested date/time is required.")
    @Future(message = "Requested date/time must be in the future.")
    private LocalDateTime requestedAt;

    private boolean pickupRequested;

    private String pickupAddress;

    @DecimalMin(value = "-90.0", message = "Pickup latitude must be between -90 and 90.")
    @DecimalMax(value = "90.0", message = "Pickup latitude must be between -90 and 90.")
    private BigDecimal pickupLatitude;

    @DecimalMin(value = "-180.0", message = "Pickup longitude must be between -180 and 180.")
    @DecimalMax(value = "180.0", message = "Pickup longitude must be between -180 and 180.")
    private BigDecimal pickupLongitude;

    private String pickupContactNumber;

    /** Free-form notes from the call — e.g. what the customer said on the phone. */
    private String notes;
}
