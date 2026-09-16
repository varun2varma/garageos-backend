package com.garageos.modules.booking.service.impl;

import com.garageos.core.enums.booking.BookingStatus;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.enums.navigation.NavigationRequestType;
import com.garageos.core.exception.BusinessException;
import com.garageos.modules.booking.dto.request.CreateBookingRequest;
import com.garageos.modules.booking.dto.response.BookingResponse;
import com.garageos.modules.booking.entity.Booking;
import com.garageos.modules.booking.repository.BookingRepository;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pickup location must be coordinates, not free text.
 *
 * A booking previously stored pickup location as pickup_address only, so
 * a driver dispatched to "Kukatpally" had a place name and nothing to
 * navigate to. navigation_request had gained latitude/longitude columns
 * in V30, but nothing upstream ever produced them and the entity never
 * mapped them, so they stayed permanently null.
 */
@ExtendWith(MockitoExtension.class)
class BookingPickupCoordinatesTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private GarageRepository garageRepository;
    @Mock private NavigationRequestService navigationRequestService;

    @InjectMocks
    private BookingServiceImpl service;

    private static final Long CUSTOMER_ID = 100L;
    private static final Long VEHICLE_ID = 5L;
    private static final Long GARAGE_ID = 10L;
    private static final String MOBILE = "9999999999";

    private static final BigDecimal LAT = new BigDecimal("17.4948570");
    private static final BigDecimal LNG = new BigDecimal("78.3996300");

    private Customer customer;
    private Vehicle vehicle;
    private Garage garage;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setMobileNumber(MOBILE);

        vehicle = new Vehicle();
        vehicle.setId(VEHICLE_ID);
        vehicle.setCustomer(customer);

        garage = new Garage();
        garage.setId(GARAGE_ID);

        GarageUserPrincipal principal = new GarageUserPrincipal(
                1L, null, "cust", "hash", "First", "Last", "c@test.io", MOBILE,
                false, UserStatus.ACTIVE, Set.of("CUSTOMER"), Set.of(), List.of());

        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        lenient().when(customerRepository.findByMobileNumber(MOBILE))
                .thenReturn(Optional.of(customer));
        lenient().when(vehicleRepository.findById(VEHICLE_ID))
                .thenReturn(Optional.of(vehicle));
        lenient().when(garageRepository.findById(GARAGE_ID))
                .thenReturn(Optional.of(garage));
        lenient().when(bookingRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private CreateBookingRequest request(boolean pickup, BigDecimal lat, BigDecimal lng) {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setVehicleId(VEHICLE_ID);
        request.setGarageId(GARAGE_ID);
        request.setServiceDescription("General service");
        request.setRequestedAt(LocalDateTime.now().plusDays(1));
        request.setPickupRequested(pickup);
        request.setPickupLatitude(lat);
        request.setPickupLongitude(lng);
        request.setPickupAddress(pickup ? "Near the metro pillar" : null);
        return request;
    }

    // ---- Creation ----

    @Test
    void pickupBooking_persistsTheSelectedCoordinates() {

        service.createBooking(request(true, LAT, LNG));

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());

        assertThat(captor.getValue().getPickupLatitude()).isEqualByComparingTo(LAT);
        assertThat(captor.getValue().getPickupLongitude()).isEqualByComparingTo(LNG);
        assertThat(captor.getValue().isPickupRequested()).isTrue();
    }

    @Test
    void pickupBooking_keepsTheAddressAsDescriptiveMetadata() {

        // The address is still stored and shown - it is simply not the
        // thing a driver navigates by.
        service.createBooking(request(true, LAT, LNG));

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());

        assertThat(captor.getValue().getPickupAddress())
                .isEqualTo("Near the metro pillar");
    }

    @Test
    void pickupBooking_withoutCoordinates_isRejected() {

        assertThatThrownBy(() -> service.createBooking(request(true, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pickup location");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void pickupBooking_withOnlyOneCoordinate_isRejected() {

        assertThatThrownBy(() -> service.createBooking(request(true, LAT, null)))
                .isInstanceOf(BusinessException.class);

        assertThatThrownBy(() -> service.createBooking(request(true, null, LNG)))
                .isInstanceOf(BusinessException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void pickupBooking_withAnAddressButNoCoordinates_isStillRejected() {

        // This is the exact defect: a typed place name used to be enough
        // to book a pickup, leaving the driver with nothing navigable.
        CreateBookingRequest request = request(true, null, null);
        request.setPickupAddress("Kukatpally");

        assertThatThrownBy(() -> service.createBooking(request))
                .isInstanceOf(BusinessException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void nonPickupBooking_needsNoCoordinatesAndStoresNone() {

        service.createBooking(request(false, null, null));

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());

        assertThat(captor.getValue().getPickupLatitude()).isNull();
        assertThat(captor.getValue().getPickupLongitude()).isNull();
    }

    @Test
    void nonPickupBooking_doesNotStoreStrayCoordinates() {

        // A client that sends coordinates on a drop-off booking must not
        // have them silently persisted.
        service.createBooking(request(false, LAT, LNG));

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());

        assertThat(captor.getValue().getPickupLatitude()).isNull();
        assertThat(captor.getValue().getPickupLongitude()).isNull();
    }

    @Test
    void bookingResponse_exposesTheCoordinates() {

        BookingResponse response = service.createBooking(request(true, LAT, LNG));

        assertThat(response.getPickupLatitude()).isEqualByComparingTo(LAT);
        assertThat(response.getPickupLongitude()).isEqualByComparingTo(LNG);
    }

    // ---- Propagation to the pickup trip ----

    @Test
    void confirmingAPickupBooking_carriesTheCoordinatesOntoTheNavigationRequest() {

        Booking booking = Booking.builder()
                .id(1L)
                .customerId(CUSTOMER_ID)
                .vehicleId(VEHICLE_ID)
                .garageId(GARAGE_ID)
                .serviceDescription("General service")
                .requestedAt(LocalDateTime.now().plusDays(1))
                .pickupRequested(true)
                .pickupAddress("Near the metro pillar")
                .pickupLatitude(LAT)
                .pickupLongitude(LNG)
                .status(BookingStatus.REQUESTED)
                .build();

        GarageUserPrincipal staff = new GarageUserPrincipal(
                2L, GARAGE_ID, "mgr", "hash", "First", "Last", "m@test.io", "8888888888",
                false, UserStatus.ACTIVE, Set.of("MANAGER"), Set.of(), List.of());

        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(staff);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Garage-scoped lookup: a manager only ever resolves bookings
        // belonging to their own garage.
        when(bookingRepository.findByIdAndGarageId(1L, GARAGE_ID))
                .thenReturn(Optional.of(booking));
        when(navigationRequestService.createRequest(anyLong(), any()))
                .thenReturn(NavigationRequestResponse.builder().id(55L).build());

        service.confirmBooking(1L, "See you then");

        ArgumentCaptor<CreateNavigationRequest> captor =
                ArgumentCaptor.forClass(CreateNavigationRequest.class);
        verify(navigationRequestService).createRequest(anyLong(), captor.capture());

        CreateNavigationRequest created = captor.getValue();
        assertThat(created.getRequestType()).isEqualTo(NavigationRequestType.PICKUP);
        assertThat(created.getPickupLatitude()).isEqualByComparingTo(LAT);
        assertThat(created.getPickupLongitude()).isEqualByComparingTo(LNG);

        // And the address still travels with them, as metadata.
        assertThat(created.getPickupAddress()).isEqualTo("Near the metro pillar");

        assertThat(booking.getNavigationRequestId()).isEqualTo(55L);
    }
}
