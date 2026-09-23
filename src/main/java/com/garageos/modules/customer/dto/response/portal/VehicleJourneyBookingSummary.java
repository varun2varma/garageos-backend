package com.garageos.modules.customer.dto.response.portal;

import com.garageos.core.enums.booking.BookingStatus;
import lombok.Builder;
import lombok.Data;

/**
 * Deliberately lean subset of Booking - just enough for the active
 * vehicle journey card. The full booking (addresses, coordinates,
 * remarks, garage/customer names) stays behind the existing
 * {@code /bookings/**} endpoints; this is not a replacement for
 * BookingResponse.
 */
@Data
@Builder
public class VehicleJourneyBookingSummary {

    private Long id;

    private BookingStatus status;

    private boolean pickupRequested;
}
