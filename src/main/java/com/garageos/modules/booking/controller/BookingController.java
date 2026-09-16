package com.garageos.modules.booking.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.booking.dto.request.BookingDecisionRequest;
import com.garageos.modules.booking.dto.request.CreateBookingRequest;
import com.garageos.modules.booking.dto.response.BookingResponse;
import com.garageos.modules.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /** Customer: create a service-request booking for their own vehicle. */
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request) {

        return ApiResponseUtil.created(
                "Booking created successfully.",
                bookingService.createBooking(request)
        );
    }

    /** Customer: their own bookings only. */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings() {

        return ApiResponseUtil.success(
                "Bookings fetched successfully.",
                bookingService.getMyBookings()
        );
    }

    /** Garage operational staff: bookings for their own garage only. */
    @GetMapping("/garage")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getGarageBookings() {

        return ApiResponseUtil.success(
                "Bookings fetched successfully.",
                bookingService.getGarageBookings()
        );
    }

    /** Customer (own booking) or garage staff (own garage's booking). */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(@PathVariable Long id) {

        return ApiResponseUtil.success(
                "Booking fetched successfully.",
                bookingService.getBooking(id)
        );
    }

    /** Garage: accept a REQUESTED booking; creates a pickup NavigationRequest when requested. */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @PathVariable Long id,
            @RequestBody(required = false) BookingDecisionRequest request) {

        String remarks = request == null ? null : request.getRemarks();

        return ApiResponseUtil.success(
                "Booking confirmed successfully.",
                bookingService.confirmBooking(id, remarks)
        );
    }

    /** Garage: reject a REQUESTED booking. */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<BookingResponse>> rejectBooking(
            @PathVariable Long id,
            @RequestBody(required = false) BookingDecisionRequest request) {

        String remarks = request == null ? null : request.getRemarks();

        return ApiResponseUtil.success(
                "Booking rejected successfully.",
                bookingService.rejectBooking(id, remarks)
        );
    }

    /** Customer: cancel their own booking while REQUESTED or CONFIRMED. */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(@PathVariable Long id) {

        return ApiResponseUtil.success(
                "Booking cancelled successfully.",
                bookingService.cancelBooking(id)
        );
    }
}
