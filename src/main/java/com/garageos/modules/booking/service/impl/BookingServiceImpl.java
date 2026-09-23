package com.garageos.modules.booking.service.impl;

import com.garageos.core.enums.booking.BookingStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.enums.navigation.NavigationRequestType;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.enums.audit.AuditEventType;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.audit.service.AuditService;
import com.garageos.core.enums.booking.BookingSource;
import com.garageos.modules.booking.dto.request.CreateBookingRequest;
import com.garageos.modules.booking.dto.request.CreatePhoneBookingRequest;
import com.garageos.modules.booking.dto.response.BookingResponse;
import com.garageos.modules.booking.entity.Booking;
import com.garageos.modules.booking.repository.BookingRepository;
import com.garageos.modules.booking.service.BookingService;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.navigation.dto.request.CreateNavigationRequest;
import com.garageos.modules.navigation.dto.response.NavigationRequestResponse;
import com.garageos.modules.navigation.service.NavigationRequestService;
import com.garageos.modules.vehicle.entity.Vehicle;
import com.garageos.modules.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Customer service-request intent, kept deliberately separate from
 * NavigationRequest (logistics) and JobCard (actual garage work). A
 * confirmed booking with pickup requested creates a NavigationRequest via
 * the existing NavigationRequestService rather than a second trip system —
 * see {@link #confirmBooking}.
 */
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final GarageRepository garageRepository;
    private final NavigationRequestService navigationRequestService;
    private final AuditService auditService;

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {

        Customer customer = currentCustomer();

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found with id : " + request.getVehicleId()));

        // A customer may only book service for their own vehicle - never
        // trust the vehicleId alone.
        if (vehicle.getCustomer() == null
                || !vehicle.getCustomer().getId().equals(customer.getId())) {
            throw new ResourceNotFoundException(
                    "Vehicle not found with id : " + request.getVehicleId());
        }

        Garage garage = garageRepository.findById(request.getGarageId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Garage not found with id : " + request.getGarageId()));

        if (request.isPickupRequested()) {

            // Coordinates are the canonical navigation location, so they
            // are what a pickup booking genuinely requires. The address is
            // descriptive metadata and stays optional: a customer who
            // drops a pin on a spot with no useful street name must still
            // be able to book.
            if (request.getPickupLatitude() == null
                    || request.getPickupLongitude() == null) {

                throw new BusinessException(
                        "A pickup location must be selected when pickup is requested.");
            }
        }

        Booking booking = Booking.builder()
                .customerId(customer.getId())
                .vehicleId(vehicle.getId())
                .garageId(garage.getId())
                .serviceDescription(request.getServiceDescription())
                .concerns(request.getConcerns())
                .requestedAt(request.getRequestedAt())
                .pickupRequested(request.isPickupRequested())
                .pickupAddress(request.getPickupAddress())
                // Only ever stored for a pickup booking - a drop-off
                // booking must not carry a stray location.
                .pickupLatitude(request.isPickupRequested()
                        ? request.getPickupLatitude() : null)
                .pickupLongitude(request.isPickupRequested()
                        ? request.getPickupLongitude() : null)
                .pickupContactNumber(request.isPickupRequested()
                        ? request.getPickupContactNumber() : null)
                .status(BookingStatus.REQUESTED)
                .build();

        booking = bookingRepository.save(booking);

        auditService.record(
                AuditEventType.BOOKING_CREATED,
                "Booking",
                booking.getId(),
                booking.getGarageId(),
                java.util.Map.of("pickupRequested", booking.isPickupRequested())
        );

        return toResponse(booking, customer, vehicle, garage);
    }

    @Override
    @Transactional
    public BookingResponse createPhoneBooking(CreatePhoneBookingRequest request) {

        Long garageId = currentEmployeeGarageId();

        // Deliberately never creates a customer - "do not create
        // duplicate customers unnecessarily" is satisfied by requiring
        // one to already exist (via the existing employee customer-CRUD
        // flow, features/customer/) rather than this endpoint guessing
        // whether a phone number is a genuinely new customer.
        Customer customer = customerRepository.findByMobileNumber(request.getCustomerMobile().trim())
                .orElseThrow(() -> new BusinessException(
                        "No customer found with mobile number " + request.getCustomerMobile()
                                + ". Please create the customer first."));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found with id : " + request.getVehicleId()));

        if (vehicle.getCustomer() == null
                || !vehicle.getCustomer().getId().equals(customer.getId())) {
            throw new ResourceNotFoundException(
                    "Vehicle not found with id : " + request.getVehicleId());
        }

        Garage garage = garageRepository.findById(garageId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Garage not found with id : " + garageId));

        if (request.isPickupRequested()
                && (request.getPickupLatitude() == null || request.getPickupLongitude() == null)) {

            throw new BusinessException(
                    "A pickup location must be selected when pickup is requested.");
        }

        GarageUserPrincipal principal = currentPrincipal();

        Booking booking = Booking.builder()
                .customerId(customer.getId())
                .vehicleId(vehicle.getId())
                .garageId(garage.getId())
                .serviceDescription(request.getServiceDescription())
                .concerns(request.getConcerns())
                .requestedAt(request.getRequestedAt())
                .pickupRequested(request.isPickupRequested())
                .pickupAddress(request.getPickupAddress())
                .pickupLatitude(request.isPickupRequested() ? request.getPickupLatitude() : null)
                .pickupLongitude(request.isPickupRequested() ? request.getPickupLongitude() : null)
                .pickupContactNumber(request.isPickupRequested() ? request.getPickupContactNumber() : null)
                .source(BookingSource.PHONE)
                .createdByEmployeeId(principal.getId())
                .notes(request.getNotes())
                .status(BookingStatus.REQUESTED)
                .build();

        booking = bookingRepository.save(booking);

        auditService.record(
                AuditEventType.BOOKING_CREATED,
                "Booking",
                booking.getId(),
                booking.getGarageId(),
                java.util.Map.of(
                        "pickupRequested", booking.isPickupRequested(),
                        "source", BookingSource.PHONE,
                        "createdByEmployeeId", principal.getId()
                )
        );

        return toResponse(booking, customer, vehicle, garage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings() {

        Customer customer = currentCustomer();

        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getGarageBookings() {

        Long garageId = currentEmployeeGarageId();

        return bookingRepository.findByGarageIdOrderByRequestedAtAsc(garageId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long id) {

        GarageUserPrincipal principal = currentPrincipal();

        if (principal.getRoles().contains(RoleCode.CUSTOMER.name())) {

            Customer customer = currentCustomer();

            Booking booking = bookingRepository.findByIdAndCustomerId(id, customer.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Booking not found with id : " + id));

            return toResponse(booking);
        }

        Long garageId = currentEmployeeGarageId();

        Booking booking = bookingRepository.findByIdAndGarageId(id, garageId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found with id : " + id));

        return toResponse(booking);
    }

    /**
     * Accepted booking, per product spec: garage acceptance is the signal
     * that lets pickup logistics proceed. When pickup was requested, this
     * creates the corresponding NavigationRequest (reusing the existing
     * Navigation module) and records its id on the booking — it does NOT
     * create a Job Card; that remains a separate, existing, employee action
     * once the vehicle is actually received.
     */
    @Override
    @Transactional
    public BookingResponse confirmBooking(Long id, String remarks) {

        Booking booking = ownedGarageBooking(id);

        requireStatus(booking, BookingStatus.REQUESTED);

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setGarageRemarks(remarks);

        if (booking.isPickupRequested()) {

            CreateNavigationRequest navigationRequest = new CreateNavigationRequest();
            navigationRequest.setVehicleId(booking.getVehicleId());
            navigationRequest.setGarageId(booking.getGarageId());
            navigationRequest.setRequestType(NavigationRequestType.PICKUP);
            navigationRequest.setPickupAddress(booking.getPickupAddress());

            // Carry the canonical coordinates onto the NavigationRequest.
            // CreateNavigationRequest has always had these fields; nothing
            // ever populated them, which is why every pickup trip reached
            // its driver with an address and no location.
            navigationRequest.setPickupLatitude(booking.getPickupLatitude());
            navigationRequest.setPickupLongitude(booking.getPickupLongitude());

            navigationRequest.setScheduledAt(booking.getRequestedAt());

            NavigationRequestResponse created =
                    navigationRequestService.createRequest(booking.getCustomerId(), navigationRequest);

            booking.setNavigationRequestId(created.getId());
        }

        booking = bookingRepository.save(booking);

        auditService.record(
                AuditEventType.BOOKING_CONFIRMED,
                "Booking",
                booking.getId(),
                booking.getGarageId(),
                java.util.Map.of("navigationRequestId", booking.getNavigationRequestId() == null ? -1 : booking.getNavigationRequestId())
        );

        return toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse rejectBooking(Long id, String remarks) {

        Booking booking = ownedGarageBooking(id);

        requireStatus(booking, BookingStatus.REQUESTED);

        booking.setStatus(BookingStatus.REJECTED);
        booking.setGarageRemarks(remarks);

        booking = bookingRepository.save(booking);

        auditService.record(
                AuditEventType.BOOKING_REJECTED,
                "Booking",
                booking.getId(),
                booking.getGarageId(),
                remarks == null ? java.util.Map.of() : java.util.Map.of("remarks", remarks)
        );

        return toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long id) {

        Customer customer = currentCustomer();

        Booking booking = bookingRepository.findByIdAndCustomerId(id, customer.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found with id : " + id));

        if (booking.getStatus() != BookingStatus.REQUESTED
                && booking.getStatus() != BookingStatus.CONFIRMED) {

            throw new BusinessException(
                    "A booking can only be cancelled while requested or confirmed.");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        booking = bookingRepository.save(booking);

        auditService.record(
                AuditEventType.BOOKING_CANCELLED,
                "Booking",
                booking.getId(),
                booking.getGarageId(),
                java.util.Map.of()
        );

        return toResponse(booking);
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private void requireStatus(Booking booking, BookingStatus expected) {

        if (booking.getStatus() != expected) {
            throw new BusinessException(
                    "Booking is not in a state that allows this action.");
        }
    }

    private Booking ownedGarageBooking(Long id) {

        Long garageId = currentEmployeeGarageId();

        return bookingRepository.findByIdAndGarageId(id, garageId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found with id : " + id));
    }

    private GarageUserPrincipal currentPrincipal() {

        return (GarageUserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private Customer currentCustomer() {

        GarageUserPrincipal principal = currentPrincipal();

        return customerRepository.findByMobileNumber(principal.getMobile())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));
    }

    /** Never trusts a client-supplied garageId - always the authenticated employee's own. */
    private Long currentEmployeeGarageId() {

        GarageUserPrincipal principal = currentPrincipal();

        if (principal.getGarageId() == null) {
            throw new BusinessException("No garage context for this account.");
        }

        return principal.getGarageId();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private BookingResponse toResponse(Booking booking) {

        Customer customer = customerRepository.findById(booking.getCustomerId()).orElse(null);
        Vehicle vehicle = vehicleRepository.findById(booking.getVehicleId()).orElse(null);
        Garage garage = garageRepository.findById(booking.getGarageId()).orElse(null);

        return toResponse(booking, customer, vehicle, garage);
    }

    private BookingResponse toResponse(Booking booking, Customer customer, Vehicle vehicle, Garage garage) {

        return BookingResponse.builder()
                .id(booking.getId())
                .garageId(booking.getGarageId())
                .garageName(garage == null ? null : garage.getGarageName())
                .customerId(booking.getCustomerId())
                .customerName(customer == null ? null : customer.getFullName())
                .vehicleId(booking.getVehicleId())
                .registrationNumber(vehicle == null ? null : vehicle.getRegistrationNumber())
                .serviceDescription(booking.getServiceDescription())
                .concerns(booking.getConcerns())
                .requestedAt(booking.getRequestedAt())
                .pickupRequested(booking.isPickupRequested())
                .pickupAddress(booking.getPickupAddress())
                .pickupLatitude(booking.getPickupLatitude())
                .pickupLongitude(booking.getPickupLongitude())
                .pickupContactNumber(booking.getPickupContactNumber())
                .source(booking.getSource())
                .createdByEmployeeId(booking.getCreatedByEmployeeId())
                .notes(booking.getNotes())
                .status(booking.getStatus())
                .garageRemarks(booking.getGarageRemarks())
                .navigationRequestId(booking.getNavigationRequestId())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
