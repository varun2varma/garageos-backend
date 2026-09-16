package com.garageos.modules.handover.service.impl;

import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.enums.navigation.HandoverStatus;
import com.garageos.core.enums.navigation.TripLeg;
import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.core.enums.navigation.TripType;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.handover.dto.response.HandoverCodeResponse;
import com.garageos.modules.handover.dto.response.HandoverStatusResponse;
import com.garageos.modules.handover.entity.VehicleHandover;
import com.garageos.modules.handover.repository.VehicleHandoverRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.navigation.entity.NavigationRequest;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.repository.NavigationTripRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandoverServiceImplTest {

    @Mock private VehicleHandoverRepository handoverRepository;
    @Mock private NavigationTripRepository navigationTripRepository;
    @Mock private NavigationRequestRepository navigationRequestRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private HandoverServiceImpl service;

    private static final Long TRIP_ID = 700L;
    private static final Long NAV_REQUEST_ID = 300L;
    private static final Long CUSTOMER_ID = 100L;
    private static final Long DRIVER_ID = 50L;
    private static final Long GARAGE_ID = 10L;
    private static final Long OTHER_GARAGE_ID = 11L;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsCustomer(String mobile) {
        stubPrincipal(new GarageUserPrincipal(
                1L, null, "cust", "hash", "First", "Last", "c@test.io", mobile,
                false, UserStatus.ACTIVE, Set.of("CUSTOMER"), Set.of(), List.of()));
    }

    private void authenticateAsDriver(Long driverId, Long garageId) {
        stubPrincipal(new GarageUserPrincipal(
                driverId, garageId, "drv", "hash", "First", "Last", "d@test.io", "8888888888",
                false, UserStatus.ACTIVE, Set.of("DRIVER"), Set.of(), List.of()));
    }

    private void authenticateAsEmployee(Long garageId) {
        stubPrincipal(new GarageUserPrincipal(
                2L, garageId, "emp", "hash", "First", "Last", "e@test.io", "9999999999",
                false, UserStatus.ACTIVE, Set.of("MANAGER"), Set.of(), List.of()));
    }

    private void stubPrincipal(GarageUserPrincipal principal) {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private NavigationTrip arrivedTrip(TripType type) {
        return NavigationTrip.builder()
                .id(TRIP_ID)
                .navigationRequestId(NAV_REQUEST_ID)
                .vehicleId(1L)
                .driverId(DRIVER_ID)
                .tripType(type)
                .currentLeg(TripLeg.GARAGE_TO_CUSTOMER)
                .status(TripStatus.IN_PROGRESS)
                .arrivedAt(LocalDateTime.now())
                .build();
    }

    private NavigationRequest navigationRequest() {
        return NavigationRequest.builder()
                .id(NAV_REQUEST_ID)
                .customerId(CUSTOMER_ID)
                .vehicleId(1L)
                .garageId(GARAGE_ID)
                .build();
    }

    private VehicleHandover pendingHandover() {
        return VehicleHandover.builder()
                .id(1L)
                .tripId(TRIP_ID)
                .direction(TripType.PICKUP)
                .codeHash("HASHED")
                .status(HandoverStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .failedAttempts(0)
                .build();
    }

    // ---- getOrCreateActiveCode (customer) ----

    @Test
    void getOrCreateActiveCode_ownedTripArrived_issuesFreshCode() {

        authenticateAsCustomer("9000000000");
        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        when(customerRepository.findByMobileNumber("9000000000")).thenReturn(Optional.of(customer));
        when(navigationTripRepository.findById(TRIP_ID)).thenReturn(Optional.of(arrivedTrip(TripType.PICKUP)));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));
        when(handoverRepository.findFirstByTripIdAndStatusOrderByCreatedAtDesc(TRIP_ID, HandoverStatus.PENDING))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("HASHED");

        HandoverCodeResponse response = service.getOrCreateActiveCode(TRIP_ID);

        assertThat(response.getCode()).hasSize(6);
        assertThat(response.getCode()).matches("\\d{6}");
        assertThat(response.getDirection()).isEqualTo(TripType.PICKUP);

        ArgumentCaptor<VehicleHandover> captor = ArgumentCaptor.forClass(VehicleHandover.class);
        verify(handoverRepository).save(captor.capture());
        assertThat(captor.getValue().getCodeHash()).isEqualTo("HASHED");
    }

    @Test
    void getOrCreateActiveCode_notOwnedByCaller_isRejectedAsNotFound() {

        authenticateAsCustomer("9000000000");
        Customer someoneElse = new Customer();
        someoneElse.setId(999L);
        when(customerRepository.findByMobileNumber("9000000000")).thenReturn(Optional.of(someoneElse));
        when(navigationTripRepository.findById(TRIP_ID)).thenReturn(Optional.of(arrivedTrip(TripType.PICKUP)));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));

        assertThatThrownBy(() -> service.getOrCreateActiveCode(TRIP_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getOrCreateActiveCode_driverNotYetArrived_isRejected() {

        authenticateAsCustomer("9000000000");
        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        when(customerRepository.findByMobileNumber("9000000000")).thenReturn(Optional.of(customer));

        NavigationTrip notArrived = NavigationTrip.builder()
                .id(TRIP_ID).navigationRequestId(NAV_REQUEST_ID).vehicleId(1L).driverId(DRIVER_ID)
                .tripType(TripType.PICKUP).currentLeg(TripLeg.GARAGE_TO_CUSTOMER)
                .status(TripStatus.IN_PROGRESS).arrivedAt(null).build();

        when(navigationTripRepository.findById(TRIP_ID)).thenReturn(Optional.of(notArrived));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));

        assertThatThrownBy(() -> service.getOrCreateActiveCode(TRIP_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getOrCreateActiveCode_priorPendingCode_isExpiredBeforeIssuingNewOne() {

        authenticateAsCustomer("9000000000");
        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        when(customerRepository.findByMobileNumber("9000000000")).thenReturn(Optional.of(customer));
        when(navigationTripRepository.findById(TRIP_ID)).thenReturn(Optional.of(arrivedTrip(TripType.PICKUP)));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));

        VehicleHandover previous = pendingHandover();
        when(handoverRepository.findFirstByTripIdAndStatusOrderByCreatedAtDesc(TRIP_ID, HandoverStatus.PENDING))
                .thenReturn(Optional.of(previous));
        when(passwordEncoder.encode(any())).thenReturn("HASHED2");

        service.getOrCreateActiveCode(TRIP_ID);

        assertThat(previous.getStatus()).isEqualTo(HandoverStatus.EXPIRED);
        verify(handoverRepository, times(2)).save(any(VehicleHandover.class));
    }

    // ---- verify (driver) ----

    @Test
    void verify_correctCode_marksVerifiedAndCannotBeReused() {

        authenticateAsDriver(DRIVER_ID, GARAGE_ID);
        when(navigationTripRepository.findByIdAndDriverId(TRIP_ID, DRIVER_ID))
                .thenReturn(Optional.of(arrivedTrip(TripType.PICKUP)));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));

        VehicleHandover handover = pendingHandover();
        when(handoverRepository.findFirstByTripIdAndStatusOrderByCreatedAtDesc(TRIP_ID, HandoverStatus.PENDING))
                .thenReturn(Optional.of(handover));
        when(passwordEncoder.matches("123456", "HASHED")).thenReturn(true);

        HandoverStatusResponse response = service.verify(TRIP_ID, "123456");

        assertThat(response.getStatus()).isEqualTo(HandoverStatus.VERIFIED);
        assertThat(handover.getVerifiedByDriverId()).isEqualTo(DRIVER_ID);

        // Reuse: once VERIFIED, it's no longer found by the PENDING lookup,
        // so a second verify call has nothing active to check against.
        when(handoverRepository.findFirstByTripIdAndStatusOrderByCreatedAtDesc(TRIP_ID, HandoverStatus.PENDING))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify(TRIP_ID, "123456"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void verify_wrongCode_incrementsFailedAttemptsAndFails() {

        authenticateAsDriver(DRIVER_ID, GARAGE_ID);
        when(navigationTripRepository.findByIdAndDriverId(TRIP_ID, DRIVER_ID))
                .thenReturn(Optional.of(arrivedTrip(TripType.PICKUP)));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));

        VehicleHandover handover = pendingHandover();
        when(handoverRepository.findFirstByTripIdAndStatusOrderByCreatedAtDesc(TRIP_ID, HandoverStatus.PENDING))
                .thenReturn(Optional.of(handover));
        when(passwordEncoder.matches("000000", "HASHED")).thenReturn(false);

        assertThatThrownBy(() -> service.verify(TRIP_ID, "000000"))
                .isInstanceOf(BusinessException.class);

        assertThat(handover.getFailedAttempts()).isEqualTo(1);
        assertThat(handover.getStatus()).isEqualTo(HandoverStatus.PENDING);
    }

    @Test
    void verify_tooManyFailedAttempts_expiresTheCode() {

        authenticateAsDriver(DRIVER_ID, GARAGE_ID);
        when(navigationTripRepository.findByIdAndDriverId(TRIP_ID, DRIVER_ID))
                .thenReturn(Optional.of(arrivedTrip(TripType.PICKUP)));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));

        VehicleHandover handover = pendingHandover();
        handover.setFailedAttempts(5);
        when(handoverRepository.findFirstByTripIdAndStatusOrderByCreatedAtDesc(TRIP_ID, HandoverStatus.PENDING))
                .thenReturn(Optional.of(handover));

        assertThatThrownBy(() -> service.verify(TRIP_ID, "123456"))
                .isInstanceOf(BusinessException.class);

        assertThat(handover.getStatus()).isEqualTo(HandoverStatus.EXPIRED);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void verify_expiredCode_isRejected() {

        authenticateAsDriver(DRIVER_ID, GARAGE_ID);
        when(navigationTripRepository.findByIdAndDriverId(TRIP_ID, DRIVER_ID))
                .thenReturn(Optional.of(arrivedTrip(TripType.PICKUP)));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));

        VehicleHandover handover = pendingHandover();
        handover.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(handoverRepository.findFirstByTripIdAndStatusOrderByCreatedAtDesc(TRIP_ID, HandoverStatus.PENDING))
                .thenReturn(Optional.of(handover));

        assertThatThrownBy(() -> service.verify(TRIP_ID, "123456"))
                .isInstanceOf(BusinessException.class);

        assertThat(handover.getStatus()).isEqualTo(HandoverStatus.EXPIRED);
    }

    @Test
    void verify_wrongDriver_isRejectedAsNotFound() {

        authenticateAsDriver(999L, GARAGE_ID);
        when(navigationTripRepository.findByIdAndDriverId(TRIP_ID, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify(TRIP_ID, "123456"))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(handoverRepository);
    }

    @Test
    void verify_wrongGarage_isRejectedAsNotFound() {

        authenticateAsDriver(DRIVER_ID, OTHER_GARAGE_ID);
        when(navigationTripRepository.findByIdAndDriverId(TRIP_ID, DRIVER_ID))
                .thenReturn(Optional.of(arrivedTrip(TripType.PICKUP)));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));

        assertThatThrownBy(() -> service.verify(TRIP_ID, "123456"))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(handoverRepository);
    }

    @Test
    void verify_noActiveCode_isRejected() {

        authenticateAsDriver(DRIVER_ID, GARAGE_ID);
        when(navigationTripRepository.findByIdAndDriverId(TRIP_ID, DRIVER_ID))
                .thenReturn(Optional.of(arrivedTrip(TripType.PICKUP)));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));
        when(handoverRepository.findFirstByTripIdAndStatusOrderByCreatedAtDesc(TRIP_ID, HandoverStatus.PENDING))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify(TRIP_ID, "123456"))
                .isInstanceOf(BusinessException.class);
    }

    // ---- getStatus (operational) ----

    @Test
    void getStatus_wrongGarageEmployee_isRejectedAsNotFound() {

        authenticateAsEmployee(OTHER_GARAGE_ID);
        when(navigationTripRepository.findById(TRIP_ID)).thenReturn(Optional.of(arrivedTrip(TripType.DELIVERY)));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));

        assertThatThrownBy(() -> service.getStatus(TRIP_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getStatus_sameGarage_returnsStatusWithoutCode() {

        authenticateAsEmployee(GARAGE_ID);
        when(navigationTripRepository.findById(TRIP_ID)).thenReturn(Optional.of(arrivedTrip(TripType.DELIVERY)));
        when(navigationRequestRepository.findById(NAV_REQUEST_ID)).thenReturn(Optional.of(navigationRequest()));
        when(handoverRepository.findFirstByTripIdOrderByCreatedAtDesc(TRIP_ID))
                .thenReturn(Optional.of(pendingHandover()));

        HandoverStatusResponse response = service.getStatus(TRIP_ID);

        assertThat(response.getStatus()).isEqualTo(HandoverStatus.PENDING);
    }
}
