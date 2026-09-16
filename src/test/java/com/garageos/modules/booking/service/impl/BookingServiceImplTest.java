package com.garageos.modules.booking.service.impl;

import com.garageos.core.enums.booking.BookingStatus;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.enums.navigation.NavigationRequestType;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.booking.dto.request.CreateBookingRequest;
import com.garageos.modules.booking.dto.response.BookingResponse;
import com.garageos.modules.booking.entity.Booking;
import com.garageos.modules.booking.repository.BookingRepository;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.navigation.dto.response.NavigationRequestResponse;
import com.garageos.modules.navigation.service.NavigationRequestService;
import com.garageos.modules.vehicle.entity.Vehicle;
import com.garageos.modules.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private GarageRepository garageRepository;
    @Mock private NavigationRequestService navigationRequestService;

    @InjectMocks
    private BookingServiceImpl service;

    private static final Long CUSTOMER_ID = 100L;
    private static final Long OTHER_CUSTOMER_ID = 101L;
    private static final Long VEHICLE_ID = 200L;
    private static final Long GARAGE_ID = 10L;
    private static final Long OTHER_GARAGE_ID = 11L;
    private static final Long BOOKING_ID = 900L;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsCustomer(String mobile) {
        GarageUserPrincipal principal = new GarageUserPrincipal(
                1L, null, "cust", "hash", "First", "Last", "c@test.io", mobile,
                false, UserStatus.ACTIVE, Set.of("CUSTOMER"), Set.of(), List.of());
        stubPrincipal(principal);
    }

    private void authenticateAsEmployee(Long garageId) {
        GarageUserPrincipal principal = new GarageUserPrincipal(
                2L, garageId, "emp", "hash", "First", "Last", "e@test.io", "9999999999",
                false, UserStatus.ACTIVE, Set.of("MANAGER"), Set.of(), List.of());
        stubPrincipal(principal);
    }

    private void stubPrincipal(GarageUserPrincipal principal) {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private Customer customer(Long id) {
        Customer c = new Customer();
        c.setId(id);
        c.setFirstName("Ravi");
        c.setMobileNumber("9000000000");
        return c;
    }

    private Vehicle vehicle(Long id, Customer owner) {
        Vehicle v = new Vehicle();
        v.setId(id);
        v.setRegistrationNumber("KA01AB1234");
        v.setCustomer(owner);
        return v;
    }

    private Garage garage(Long id) {
        Garage g = new Garage();
        g.setId(id);
        g.setGarageName("Test Garage");
        return g;
    }

    private CreateBookingRequest request(boolean pickup) {
        CreateBookingRequest r = new CreateBookingRequest();
        r.setVehicleId(VEHICLE_ID);
        r.setGarageId(GARAGE_ID);
        r.setServiceDescription("Periodic service");
        r.setConcerns("Brake noise");
        r.setRequestedAt(LocalDateTime.now().plusDays(1));
        r.setPickupRequested(pickup);
        if (pickup) {
            r.setPickupAddress("123 MG Road");
        }
        return r;
    }

    private Booking bookingEntity(BookingStatus status, boolean pickup) {
        return Booking.builder()
                .id(BOOKING_ID)
                .customerId(CUSTOMER_ID)
                .vehicleId(VEHICLE_ID)
                .garageId(GARAGE_ID)
                .serviceDescription("Periodic service")
                .requestedAt(LocalDateTime.now().plusDays(1))
                .pickupRequested(pickup)
                .pickupAddress(pickup ? "123 MG Road" : null)
                .status(status)
                .build();
    }

    @Test
    void createBooking_ownVehicle_succeeds() {

        authenticateAsCustomer("9000000000");
        Customer customer = customer(CUSTOMER_ID);
        when(customerRepository.findByMobileNumber("9000000000")).thenReturn(Optional.of(customer));

        Vehicle vehicle = vehicle(VEHICLE_ID, customer);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(garageRepository.findById(GARAGE_ID)).thenReturn(Optional.of(garage(GARAGE_ID)));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = service.createBooking(request(false));

        assertThat(response.getStatus()).isEqualTo(BookingStatus.REQUESTED);
        assertThat(response.getCustomerId()).isEqualTo(CUSTOMER_ID);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(BookingStatus.REQUESTED);
    }

    @Test
    void createBooking_someoneElsesVehicle_isRejectedAsNotFound() {

        authenticateAsCustomer("9000000000");
        when(customerRepository.findByMobileNumber("9000000000"))
                .thenReturn(Optional.of(customer(CUSTOMER_ID)));

        Vehicle othersVehicle = vehicle(VEHICLE_ID, customer(OTHER_CUSTOMER_ID));
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(othersVehicle));

        assertThatThrownBy(() -> service.createBooking(request(false)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_pickupRequestedWithoutAddress_isRejected() {

        authenticateAsCustomer("9000000000");
        Customer customer = customer(CUSTOMER_ID);
        when(customerRepository.findByMobileNumber("9000000000")).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VEHICLE_ID, customer)));
        when(garageRepository.findById(GARAGE_ID)).thenReturn(Optional.of(garage(GARAGE_ID)));

        CreateBookingRequest req = request(true);
        req.setPickupAddress(null);

        assertThatThrownBy(() -> service.createBooking(req))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getBooking_asCustomer_cannotReadAnotherCustomersBooking() {

        authenticateAsCustomer("9000000000");
        when(customerRepository.findByMobileNumber("9000000000"))
                .thenReturn(Optional.of(customer(CUSTOMER_ID)));
        when(bookingRepository.findByIdAndCustomerId(BOOKING_ID, CUSTOMER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBooking(BOOKING_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getGarageBookings_onlyReturnsCallersOwnGarage() {

        authenticateAsEmployee(GARAGE_ID);
        when(bookingRepository.findByGarageIdOrderByRequestedAtAsc(GARAGE_ID))
                .thenReturn(List.of(bookingEntity(BookingStatus.REQUESTED, false)));
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer(CUSTOMER_ID)));
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VEHICLE_ID, customer(CUSTOMER_ID))));
        when(garageRepository.findById(GARAGE_ID)).thenReturn(Optional.of(garage(GARAGE_ID)));

        List<BookingResponse> result = service.getGarageBookings();

        assertThat(result).hasSize(1);
        verify(bookingRepository).findByGarageIdOrderByRequestedAtAsc(GARAGE_ID);
        verify(bookingRepository, never()).findByGarageIdOrderByRequestedAtAsc(OTHER_GARAGE_ID);
    }

    @Test
    void confirmBooking_wrongGarageEmployee_isRejectedAsNotFound() {

        authenticateAsEmployee(OTHER_GARAGE_ID);
        when(bookingRepository.findByIdAndGarageId(BOOKING_ID, OTHER_GARAGE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmBooking(BOOKING_ID, "ok"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(navigationRequestService, never()).createRequest(any(), any());
    }

    @Test
    void confirmBooking_notInRequestedState_throwsBusinessException() {

        authenticateAsEmployee(GARAGE_ID);
        Booking alreadyConfirmed = bookingEntity(BookingStatus.CONFIRMED, false);
        when(bookingRepository.findByIdAndGarageId(BOOKING_ID, GARAGE_ID))
                .thenReturn(Optional.of(alreadyConfirmed));

        assertThatThrownBy(() -> service.confirmBooking(BOOKING_ID, "ok"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void confirmBooking_withPickupRequested_createsNavigationRequestAndLinksIt() {

        authenticateAsEmployee(GARAGE_ID);
        Booking booking = bookingEntity(BookingStatus.REQUESTED, true);
        when(bookingRepository.findByIdAndGarageId(BOOKING_ID, GARAGE_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        NavigationRequestResponse navResponse = NavigationRequestResponse.builder().id(555L).build();
        when(navigationRequestService.createRequest(eq(CUSTOMER_ID), any())).thenReturn(navResponse);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer(CUSTOMER_ID)));
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VEHICLE_ID, customer(CUSTOMER_ID))));
        when(garageRepository.findById(GARAGE_ID)).thenReturn(Optional.of(garage(GARAGE_ID)));

        BookingResponse response = service.confirmBooking(BOOKING_ID, "confirmed");

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.getNavigationRequestId()).isEqualTo(555L);

        ArgumentCaptor<com.garageos.modules.navigation.dto.request.CreateNavigationRequest> captor =
                ArgumentCaptor.forClass(com.garageos.modules.navigation.dto.request.CreateNavigationRequest.class);
        verify(navigationRequestService).createRequest(eq(CUSTOMER_ID), captor.capture());
        assertThat(captor.getValue().getRequestType()).isEqualTo(NavigationRequestType.PICKUP);
        assertThat(captor.getValue().getPickupAddress()).isEqualTo("123 MG Road");
    }

    @Test
    void confirmBooking_withoutPickup_doesNotTouchNavigation() {

        authenticateAsEmployee(GARAGE_ID);
        Booking booking = bookingEntity(BookingStatus.REQUESTED, false);
        when(bookingRepository.findByIdAndGarageId(BOOKING_ID, GARAGE_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer(CUSTOMER_ID)));
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VEHICLE_ID, customer(CUSTOMER_ID))));
        when(garageRepository.findById(GARAGE_ID)).thenReturn(Optional.of(garage(GARAGE_ID)));

        service.confirmBooking(BOOKING_ID, null);

        verify(navigationRequestService, never()).createRequest(any(), any());
    }

    @Test
    void cancelBooking_ownedAndCancellable_succeeds() {

        authenticateAsCustomer("9000000000");
        when(customerRepository.findByMobileNumber("9000000000"))
                .thenReturn(Optional.of(customer(CUSTOMER_ID)));
        Booking booking = bookingEntity(BookingStatus.REQUESTED, false);
        when(bookingRepository.findByIdAndCustomerId(BOOKING_ID, CUSTOMER_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = service.cancelBooking(BOOKING_ID);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancelBooking_alreadyCompleted_isRejected() {

        authenticateAsCustomer("9000000000");
        when(customerRepository.findByMobileNumber("9000000000"))
                .thenReturn(Optional.of(customer(CUSTOMER_ID)));
        Booking booking = bookingEntity(BookingStatus.COMPLETED, false);
        when(bookingRepository.findByIdAndCustomerId(BOOKING_ID, CUSTOMER_ID)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.cancelBooking(BOOKING_ID))
                .isInstanceOf(BusinessException.class);
    }
}
