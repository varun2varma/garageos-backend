package com.garageos.modules.booking.dto.response;

import com.garageos.core.enums.booking.BookingStatus;
import lombok.Builder;
import lombok.Getter;

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

    private BookingStatus status;
    private String garageRemarks;

    private Long navigationRequestId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
