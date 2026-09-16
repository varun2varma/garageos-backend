package com.garageos.modules.booking.service;

import com.garageos.modules.booking.dto.request.CreateBookingRequest;
import com.garageos.modules.booking.dto.response.BookingResponse;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request);

    List<BookingResponse> getMyBookings();

    List<BookingResponse> getGarageBookings();

    /** Authorized for the owning customer or the booking's own garage's staff. */
    BookingResponse getBooking(Long id);

    BookingResponse confirmBooking(Long id, String remarks);

    BookingResponse rejectBooking(Long id, String remarks);

    BookingResponse cancelBooking(Long id);
}
