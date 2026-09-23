package com.garageos.modules.booking.dto.response;

import com.garageos.core.enums.booking.BookingSource;
import com.garageos.core.enums.booking.BookingStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class BookingResponse {

    private Long id;

    private Long garageId;
    private String garageName;

    private Long customerId;
    private String customerName;

    private Long vehicleId;
    private String registrationNumber;

    private String serviceDescription;
    private String concerns;

    private LocalDateTime requestedAt;

    private boolean pickupRequested;
    private String pickupAddress;

    /**
     * Canonical pickup coordinates — what the customer selected, what the
     * manager sees, and what the driver navigates to. Null when pickup
     * was not requested, or for bookings created before V41.
     */
    private BigDecimal pickupLatitude;
    private BigDecimal pickupLongitude;
    private String pickupContactNumber;

    private BookingSource source;
    private Long createdByEmployeeId;
    private String notes;

    private BookingStatus status;
    private String garageRemarks;

    private Long navigationRequestId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
