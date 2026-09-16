package com.garageos.modules.jobcard.service.impl;

import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.booking.BookingStatus;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.booking.entity.Booking;
import com.garageos.modules.booking.repository.BookingRepository;
import com.garageos.modules.garagemembership.repository.GarageMembershipRepository;
import com.garageos.modules.complaint.entity.Complaint;
import com.garageos.modules.complaint.mapper.ComplaintMapper;
import com.garageos.modules.complaint.service.ComplaintService;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.inspectionfinding.service.InspectionFindingService;
import com.garageos.modules.jobcard.dto.request.CreateJobCardRequest;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.mapper.JobCardMapper;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.jobcard.validator.JobCardStatusValidator;
import com.garageos.modules.qualitycheck.service.QualityCheckService;
import com.garageos.modules.estimate.service.EstimateService;
import com.garageos.modules.repairtask.repository.RepairTaskRepository;
import com.garageos.modules.vehicle.entity.Vehicle;
import com.garageos.modules.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers the canonical JobCard creation state fix: createJobCard() must
 * start a new JobCard at OPEN (not INSPECTION_PENDING), since the
 * canonical JobCardStatusValidator table only allows OPEN as the source
 * of the first real transition. Uses the real JobCardStatusValidator
 * (not mocked) so startInspection()/completeInspection() are tested
 * against the actual transition table, not an assumption about it.
 */
@ExtendWith(MockitoExtension.class)
class JobCardServiceImplTest {

    @Mock private JobCardRepository jobCardRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private GarageRepository garageRepository;
    @Mock private JobCardMapper jobCardMapper;
    @Mock private ComplaintService complaintService;
    @Mock private ComplaintMapper complaintMapper;
    @Mock private InspectionFindingService inspectionFindingService;
    @Mock private QualityCheckService qualityCheckService;
    @Mock private RepairTaskRepository repairTaskRepository;
    @Mock private EstimateService estimateService;
    @Mock private BookingRepository bookingRepository;
    @Mock private GarageMembershipRepository garageMembershipRepository;

    private final JobCardStatusValidator statusValidator = new JobCardStatusValidator();

    private JobCardServiceImpl service() {
        return new JobCardServiceImpl(
                jobCardRepository,
                vehicleRepository,
                garageRepository,
                jobCardMapper,
                statusValidator,
                complaintService,
                complaintMapper,
                inspectionFindingService,
                qualityCheckService,
                repairTaskRepository,
                estimateService,
                bookingRepository,
                garageMembershipRepository
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long garageId) {
        GarageUserPrincipal principal = new GarageUserPrincipal(
                1L, garageId, "user1", "hash", "First", "Last",
                "user@example.test", "9999999999", false, UserStatus.ACTIVE,
                Set.of("SERVICE_ADVISOR"), Set.of(), List.of()
        );
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    /**
     * Every by-number operation is tenant-scoped, so a Job Card used in a
     * transition test needs a garage and the test needs to be
     * authenticated into it. GARAGE_ID is the caller's garage throughout;
     * the cross-garage cases below authenticate elsewhere on purpose.
     */
    private static final Long GARAGE_ID = 10L;

    private JobCard jobCard(Long id, JobCardStatus status) {
        return jobCardWithGarage(id, status, GARAGE_ID);
    }

    private JobCard jobCardWithGarage(Long id, JobCardStatus status, Long garageId) {
        JobCard jc = new JobCard();
        jc.setId(id);
        jc.setStatus(status);
        Garage g = new Garage();
        g.setId(garageId);
        jc.setGarage(g);
        return jc;
    }

    @Test
    void createJobCard_startsAtOpen_notInspectionPending() {

        authenticate(10L);

        Customer customer = new Customer();
        customer.setId(1L);

        Vehicle vehicle = new Vehicle();
        vehicle.setId(5L);
        vehicle.setCustomer(customer);

        Garage garage = new Garage();
        garage.setId(10L);
        garage.setGarageCode("G010");

        CreateJobCardRequest request = new CreateJobCardRequest();
        request.setVehicleId(5L);
        request.setOdometerReading(1000L);
        request.setComplaints(List.of());

        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));
        when(garageRepository.findById(10L)).thenReturn(Optional.of(garage));
        when(jobCardRepository.findTopByGarageIdOrderByIdDesc(10L)).thenReturn(Optional.empty());
        when(jobCardMapper.toEntity(request)).thenReturn(new JobCard());
        when(complaintMapper.toEntity(request.getComplaints()))
                .thenReturn(List.<Complaint>of());

        ArgumentCaptor<JobCard> savedCaptor = ArgumentCaptor.forClass(JobCard.class);
        when(jobCardRepository.save(savedCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(jobCardMapper.toResponse(any())).thenReturn(null);

        service().createJobCard(request);

        assertThat(savedCaptor.getValue().getStatus()).isEqualTo(JobCardStatus.OPEN);
        assertThat(savedCaptor.getValue().getGarage()).isEqualTo(garage);
        assertThat(savedCaptor.getValue().getVehicle()).isEqualTo(vehicle);
        assertThat(savedCaptor.getValue().getCustomer()).isEqualTo(customer);
    }

    @Test
    void startInspection_fromOpen_transitionsToInspectionPending() {

        authenticate(GARAGE_ID);

        JobCard jc = jobCard(1L, JobCardStatus.OPEN);
        when(jobCardRepository.findByJobCardNumber("JC-1")).thenReturn(Optional.of(jc));
        when(jobCardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jobCardMapper.toResponse(any())).thenReturn(null);

        service().startInspection("JC-1");

        assertThat(jc.getStatus()).isEqualTo(JobCardStatus.INSPECTION_PENDING);
        verify(inspectionFindingService).loadInspectionTemplate(1L);
    }

    @Test
    void startInspection_fromInspectionPending_isRejected() {

        authenticate(GARAGE_ID);

        JobCard jc = jobCard(2L, JobCardStatus.INSPECTION_PENDING);
        when(jobCardRepository.findByJobCardNumber("JC-2")).thenReturn(Optional.of(jc));

        assertThatThrownBy(() -> service().startInspection("JC-2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INSPECTION_PENDING");

        assertThat(jc.getStatus()).isEqualTo(JobCardStatus.INSPECTION_PENDING);
        verify(jobCardRepository, never()).save(any());
        verifyNoInteractions(inspectionFindingService);
    }

    @Test
    void completeInspection_fromInspectionPending_transitionsToInspectionCompleted() {

        authenticate(GARAGE_ID);

        JobCard jc = jobCard(3L, JobCardStatus.INSPECTION_PENDING);
        when(jobCardRepository.findByJobCardNumber("JC-3")).thenReturn(Optional.of(jc));
        when(jobCardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jobCardMapper.toResponse(any())).thenReturn(null);

        service().completeInspection("JC-3");

        assertThat(jc.getStatus()).isEqualTo(JobCardStatus.INSPECTION_COMPLETED);
    }

    @Test
    void startInspection_fromInvalidState_isRejected() {

        authenticate(GARAGE_ID);

        JobCard jc = jobCard(4L, JobCardStatus.CLOSED);
        when(jobCardRepository.findByJobCardNumber("JC-4")).thenReturn(Optional.of(jc));

        assertThatThrownBy(() -> service().startInspection("JC-4"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(jc.getStatus()).isEqualTo(JobCardStatus.CLOSED);
    }

    // ---- Cross-garage tenant authorization (Defect #7 corrective fix) ----

    @Test
    void closeJobCard_garageA_manager_onGarageA_jobCard_isAllowed() {

        JobCard jc = jobCardWithGarage(5L, JobCardStatus.DELIVERED, 10L);
        when(jobCardRepository.findByJobCardNumber("JC-5")).thenReturn(Optional.of(jc));
        when(jobCardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jobCardMapper.toResponse(any())).thenReturn(null);

        authenticate(10L);

        service().closeJobCard("JC-5");

        assertThat(jc.getStatus()).isEqualTo(JobCardStatus.CLOSED);
    }

    @Test
    void closeJobCard_garageB_manager_onGarageA_jobCard_isDenied_withNoMutation() {

        JobCard jc = jobCardWithGarage(6L, JobCardStatus.DELIVERED, 10L);
        when(jobCardRepository.findByJobCardNumber("JC-6")).thenReturn(Optional.of(jc));

        authenticate(20L);

        assertThatThrownBy(() -> service().closeJobCard("JC-6"))
                .isInstanceOf(com.garageos.core.exception.BusinessException.class);

        assertThat(jc.getStatus()).isEqualTo(JobCardStatus.DELIVERED);
        verify(jobCardRepository, never()).save(any());
    }

    // ---- Invalid lifecycle transitions (hardening: HTTP status, not enforcement) ----

    @Test
    void closeJobCard_alreadyClosed_isRejected_withNoMutation() {

        JobCard jc = jobCardWithGarage(7L, JobCardStatus.CLOSED, 10L);
        when(jobCardRepository.findByJobCardNumber("JC-7")).thenReturn(Optional.of(jc));

        authenticate(10L);

        assertThatThrownBy(() -> service().closeJobCard("JC-7"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(jc.getStatus()).isEqualTo(JobCardStatus.CLOSED);
        verify(jobCardRepository, never()).save(any());
    }

    @Test
    void closeJobCard_fromEarlierState_isRejected_withNoMutation() {

        JobCard jc = jobCardWithGarage(8L, JobCardStatus.REPAIR_PENDING, 10L);
        when(jobCardRepository.findByJobCardNumber("JC-8")).thenReturn(Optional.of(jc));

        authenticate(10L);

        assertThatThrownBy(() -> service().closeJobCard("JC-8"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(jc.getStatus()).isEqualTo(JobCardStatus.REPAIR_PENDING);
        verify(jobCardRepository, never()).save(any());
    }

    // ---- Booking -> Job Card wiring ----

    private Vehicle vehicleFor(Long vehicleId, Long customerId) {
        Customer customer = new Customer();
        customer.setId(customerId);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setCustomer(customer);
        return vehicle;
    }

    private Booking booking(Long id, Long garageId, Long customerId, Long vehicleId, BookingStatus status) {
        return Booking.builder()
                .id(id)
                .garageId(garageId)
                .customerId(customerId)
                .vehicleId(vehicleId)
                .status(status)
                .build();
    }

    private CreateJobCardRequest requestFor(Long vehicleId, Long bookingId) {
        CreateJobCardRequest request = new CreateJobCardRequest();
        request.setVehicleId(vehicleId);
        request.setOdometerReading(1000L);
        request.setComplaints(List.of());
        request.setBookingId(bookingId);
        return request;
    }

    /** Only what's needed to reach booking resolution - the rejection tests never get further. */
    private void stubVehicleAndGarage(Vehicle vehicle, Long garageId) {
        Garage garage = new Garage();
        garage.setId(garageId);
        garage.setGarageCode("G0" + garageId);

        when(vehicleRepository.findById(vehicle.getId())).thenReturn(Optional.of(vehicle));
        when(garageRepository.findById(garageId)).thenReturn(Optional.of(garage));
    }

    /** Full happy-path stubbing, for tests expected to actually save a Job Card. */
    private void stubHappyPathExceptBooking(Vehicle vehicle, Long garageId) {
        stubVehicleAndGarage(vehicle, garageId);
        when(jobCardRepository.findTopByGarageIdOrderByIdDesc(garageId)).thenReturn(Optional.empty());
        when(jobCardMapper.toEntity(any())).thenReturn(new JobCard());
        when(complaintMapper.toEntity(anyList())).thenReturn(List.<Complaint>of());
        when(jobCardMapper.toResponse(any())).thenReturn(null);
    }

    @Test
    void createJobCard_withoutBooking_stillWorks_bookingIdRemainsNull() {

        authenticate(10L);
        Vehicle vehicle = vehicleFor(5L, 1L);
        stubHappyPathExceptBooking(vehicle, 10L);

        ArgumentCaptor<JobCard> captor = ArgumentCaptor.forClass(JobCard.class);
        when(jobCardRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service().createJobCard(requestFor(5L, null));

        assertThat(captor.getValue().getBookingId()).isNull();
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void createJobCard_withConfirmedBooking_populatesBookingIdAndCompletesBooking() {

        authenticate(10L);
        Vehicle vehicle = vehicleFor(5L, 1L);
        stubHappyPathExceptBooking(vehicle, 10L);

        Booking booking = booking(900L, 10L, 1L, 5L, BookingStatus.CONFIRMED);
        when(bookingRepository.findById(900L)).thenReturn(Optional.of(booking));
        when(jobCardRepository.existsByBookingId(900L)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<JobCard> captor = ArgumentCaptor.forClass(JobCard.class);
        when(jobCardRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service().createJobCard(requestFor(5L, 900L));

        assertThat(captor.getValue().getBookingId()).isEqualTo(900L);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        verify(bookingRepository).save(booking);
    }

    @Test
    void createJobCard_bookingFromAnotherGarage_isRejectedAsNotFound_noMutation() {

        authenticate(10L);
        Vehicle vehicle = vehicleFor(5L, 1L);
        stubVehicleAndGarage(vehicle, 10L);

        Booking booking = booking(900L, 20L, 1L, 5L, BookingStatus.CONFIRMED);
        when(bookingRepository.findById(900L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service().createJobCard(requestFor(5L, 900L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jobCardRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createJobCard_bookingForDifferentCustomer_isRejected() {

        authenticate(10L);
        // Vehicle belongs to customer 1, but the booking was made by customer 2 -
        // same vehicleId coincidentally (shouldn't normally happen, but the
        // customer check must still catch it independently of the vehicle check).
        Vehicle vehicle = vehicleFor(5L, 1L);
        stubVehicleAndGarage(vehicle, 10L);

        Booking booking = booking(900L, 10L, 2L, 5L, BookingStatus.CONFIRMED);
        when(bookingRepository.findById(900L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service().createJobCard(requestFor(5L, 900L)))
                .isInstanceOf(BusinessException.class);

        verify(jobCardRepository, never()).save(any());
    }

    @Test
    void createJobCard_bookingForDifferentVehicle_isRejected() {

        authenticate(10L);
        Vehicle vehicle = vehicleFor(5L, 1L);
        stubVehicleAndGarage(vehicle, 10L);

        Booking booking = booking(900L, 10L, 1L, 999L, BookingStatus.CONFIRMED);
        when(bookingRepository.findById(900L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service().createJobCard(requestFor(5L, 900L)))
                .isInstanceOf(BusinessException.class);

        verify(jobCardRepository, never()).save(any());
    }

    @Test
    void createJobCard_rejectedBooking_cannotCreateJobCard() {

        authenticate(10L);
        Vehicle vehicle = vehicleFor(5L, 1L);
        stubVehicleAndGarage(vehicle, 10L);

        Booking booking = booking(900L, 10L, 1L, 5L, BookingStatus.REJECTED);
        when(bookingRepository.findById(900L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service().createJobCard(requestFor(5L, 900L)))
                .isInstanceOf(BusinessException.class);

        verify(jobCardRepository, never()).save(any());
    }

    @Test
    void createJobCard_cancelledBooking_cannotCreateJobCard() {

        authenticate(10L);
        Vehicle vehicle = vehicleFor(5L, 1L);
        stubVehicleAndGarage(vehicle, 10L);

        Booking booking = booking(900L, 10L, 1L, 5L, BookingStatus.CANCELLED);
        when(bookingRepository.findById(900L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service().createJobCard(requestFor(5L, 900L)))
                .isInstanceOf(BusinessException.class);

        verify(jobCardRepository, never()).save(any());
    }

    @Test
    void createJobCard_bookingAlreadyUsedByAnotherJobCard_isRejected() {

        authenticate(10L);
        Vehicle vehicle = vehicleFor(5L, 1L);
        stubVehicleAndGarage(vehicle, 10L);

        Booking booking = booking(900L, 10L, 1L, 5L, BookingStatus.CONFIRMED);
        when(bookingRepository.findById(900L)).thenReturn(Optional.of(booking));
        when(jobCardRepository.existsByBookingId(900L)).thenReturn(true);

        assertThatThrownBy(() -> service().createJobCard(requestFor(5L, 900L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already been created");

        verify(jobCardRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
    }

    // ---- getAllJobCards garage scoping (corrective fix: previously unscoped) ----

    @Test
    void getAllJobCards_default_scopesToCallersOwnGarage() {

        authenticate(10L);
        when(jobCardRepository.findByGarage_IdIn(eq(List.of(10L)), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        service().getAllJobCards(0, 10, "id", "asc", null, false, null);

        verify(jobCardRepository).findByGarage_IdIn(eq(List.of(10L)), any());
    }

    @Test
    void getAllJobCards_requestedGarageWithoutMembership_isRejected() {

        authenticate(10L);
        when(garageMembershipRepository.existsByGarage_IdAndUser_Id(20L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service().getAllJobCards(0, 10, "id", "asc", 20L, false, null))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jobCardRepository, never()).findByGarage_IdIn(any(), any());
    }

    @Test
    void getAllJobCards_allMyGarages_scopesToEveryActiveMembership() {

        authenticate(10L);

        com.garageos.modules.garagemembership.entity.GarageMembership membershipA =
                new com.garageos.modules.garagemembership.entity.GarageMembership();
        membershipA.setGarage(garageWithId(10L));
        membershipA.setStatus(com.garageos.core.enums.garagemembership.GarageMembershipStatus.ACTIVE);

        com.garageos.modules.garagemembership.entity.GarageMembership membershipB =
                new com.garageos.modules.garagemembership.entity.GarageMembership();
        membershipB.setGarage(garageWithId(11L));
        membershipB.setStatus(com.garageos.core.enums.garagemembership.GarageMembershipStatus.ACTIVE);

        when(garageMembershipRepository.findByUser_Id(1L)).thenReturn(List.of(membershipA, membershipB));
        when(jobCardRepository.findByGarage_IdIn(eq(List.of(10L, 11L)), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        service().getAllJobCards(0, 10, "id", "asc", null, true, null);

        verify(jobCardRepository).findByGarage_IdIn(eq(List.of(10L, 11L)), any());
    }

    // ---- Cross-garage tenant scoping of EVERY by-number operation ----

    /**
     * Before this, only closeJobCard carried a tenant check. Job Card
     * numbers are sequential and garage-prefixed, so an employee in one
     * garage could read and drive the whole lifecycle of another garage's
     * Job Card just by using its number. The check now lives in the one
     * shared lookup, which is what this suite pins: if it is ever moved
     * back out to individual methods, the gaps reopen silently.
     */
    @Test
    void byNumberOperations_onAnotherGaragesJobCard_areAllDenied_withNoMutation() {

        authenticate(20L);

        final Map<String, Runnable> operations = new LinkedHashMap<>();
        operations.put("getJobCardByNumber", () -> service().getJobCardByNumber("JC-X"));
        operations.put("startInspection", () -> service().startInspection("JC-X"));
        operations.put("completeInspection", () -> service().completeInspection("JC-X"));
        operations.put("prepareEstimate", () -> service().prepareEstimate("JC-X"));
        operations.put("startRepair", () -> service().startRepair("JC-X"));
        operations.put("completeRepair", () -> service().completeRepair("JC-X"));
        operations.put("readyForDelivery", () -> service().readyForDelivery("JC-X"));
        operations.put("closeJobCard", () -> service().closeJobCard("JC-X"));

        operations.forEach((name, operation) -> {
            JobCard foreign = jobCardWithGarage(99L, JobCardStatus.DELIVERED, GARAGE_ID);
            when(jobCardRepository.findByJobCardNumber("JC-X")).thenReturn(Optional.of(foreign));

            assertThatThrownBy(operation::run)
                    .as("%s must refuse another garage's Job Card", name)
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("does not belong to your garage");

            assertThat(foreign.getStatus())
                    .as("%s must not mutate another garage's Job Card", name)
                    .isEqualTo(JobCardStatus.DELIVERED);
        });

        verify(jobCardRepository, never()).save(any());
    }

    @Test
    void byNumberOperation_forACallerWithNoGarage_isDenied() {

        // A principal with no garage context (e.g. a CUSTOMER account)
        // must not fall through the check as if it matched.
        authenticate(null);

        JobCard jc = jobCardWithGarage(98L, JobCardStatus.DELIVERED, GARAGE_ID);
        when(jobCardRepository.findByJobCardNumber("JC-N")).thenReturn(Optional.of(jc));

        assertThatThrownBy(() -> service().closeJobCard("JC-N"))
                .isInstanceOf(BusinessException.class);

        verify(jobCardRepository, never()).save(any());
    }

    @Test
    void byNumberOperation_onAJobCardWithNoGarage_isDenied() {

        // A Job Card with no garage belongs to nobody; it must not be
        // treated as belonging to whoever asks.
        authenticate(GARAGE_ID);

        JobCard orphan = new JobCard();
        orphan.setId(97L);
        orphan.setStatus(JobCardStatus.DELIVERED);
        when(jobCardRepository.findByJobCardNumber("JC-O")).thenReturn(Optional.of(orphan));

        assertThatThrownBy(() -> service().closeJobCard("JC-O"))
                .isInstanceOf(BusinessException.class);

        verify(jobCardRepository, never()).save(any());
    }

    // ---- getAllJobCards status filtering (operational Job list chips) ----

    @Test
    void getAllJobCards_withStatuses_filtersInTheDatabase_stillGarageScoped() {

        authenticate(10L);

        List<JobCardStatus> statuses = List.of(
                JobCardStatus.REPAIR_PENDING,
                JobCardStatus.REPAIR_IN_PROGRESS);

        when(jobCardRepository.findByGarage_IdInAndStatusIn(
                eq(List.of(10L)), eq(statuses), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        service().getAllJobCards(0, 10, "id", "asc", null, false, statuses);

        verify(jobCardRepository).findByGarage_IdInAndStatusIn(
                eq(List.of(10L)), eq(statuses), any());
        verify(jobCardRepository, never()).findByGarage_IdIn(any(), any());
    }

    @Test
    void getAllJobCards_withEmptyStatuses_behavesExactlyAsBeforeTheFilterExisted() {

        authenticate(10L);
        when(jobCardRepository.findByGarage_IdIn(eq(List.of(10L)), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        service().getAllJobCards(0, 10, "id", "asc", null, false, List.of());

        verify(jobCardRepository).findByGarage_IdIn(eq(List.of(10L)), any());
        verify(jobCardRepository, never())
                .findByGarage_IdInAndStatusIn(any(), any(), any());
    }

    /**
     * A status filter must never widen tenant scope: asking for a status
     * in a garage the caller has no membership in is still rejected before
     * any query runs.
     */
    @Test
    void getAllJobCards_withStatuses_doesNotBypassGarageMembershipCheck() {

        authenticate(10L);
        when(garageMembershipRepository.existsByGarage_IdAndUser_Id(20L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service().getAllJobCards(
                0, 10, "id", "asc", 20L, false, List.of(JobCardStatus.OPEN)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jobCardRepository, never())
                .findByGarage_IdInAndStatusIn(any(), any(), any());
    }

    private Garage garageWithId(Long id) {
        Garage g = new Garage();
        g.setId(id);
        return g;
    }
}
