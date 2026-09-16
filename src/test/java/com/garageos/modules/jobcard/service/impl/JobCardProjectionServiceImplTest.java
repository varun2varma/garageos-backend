package com.garageos.modules.jobcard.service.impl;

import com.garageos.core.enums.JobAssignmentStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.modules.complaint.service.ComplaintService;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.customer.service.CustomerService;
import com.garageos.modules.delivery.mapper.DeliveryMapper;
import com.garageos.modules.delivery.repository.DeliveryRepository;
import com.garageos.modules.estimate.service.EstimateService;
import com.garageos.modules.estimateitem.service.EstimateItemService;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.invoice.service.InvoiceService;
import com.garageos.modules.jobassignment.dto.response.JobAssignmentResponse;
import com.garageos.modules.jobassignment.entity.JobAssignment;
import com.garageos.modules.jobassignment.repository.JobAssignmentRepository;
import com.garageos.modules.jobassignment.service.JobAssignmentService;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.mapper.JobCardMapper;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.jobcard.validator.JobCardStatusValidator;
import com.garageos.modules.qualitycheck.mapper.QualityCheckMapper;
import com.garageos.modules.qualitycheck.repository.QualityCheckRepository;
import com.garageos.modules.repairtask.service.RepairTaskService;
import com.garageos.modules.vehicle.service.VehicleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Corrective fix for audit Finding P1: a technician viewing the
 * projection must only see their own JobAssignment records, not every
 * technician's on the same JobCard.
 */
@ExtendWith(MockitoExtension.class)
class JobCardProjectionServiceImplTest {

    @Mock private JobCardRepository jobCardRepository;
    @Mock private JobCardMapper jobCardMapper;
    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerService customerService;
    @Mock private VehicleService vehicleService;
    @Mock private ComplaintService complaintService;
    @Mock private EstimateService estimateService;
    @Mock private EstimateItemService estimateItemService;
    @Mock private RepairTaskService repairTaskService;
    @Mock private JobAssignmentService jobAssignmentService;
    @Mock private JobAssignmentRepository jobAssignmentRepository;
    @Mock private QualityCheckRepository qualityCheckRepository;
    @Mock private QualityCheckMapper qualityCheckMapper;
    @Mock private InvoiceService invoiceService;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryMapper deliveryMapper;

    @InjectMocks
    private JobCardProjectionServiceImpl service;

    private static final Long JOB_CARD_ID = 500L;
    private static final Long GARAGE_ID = 10L;
    private static final Long TECHNICIAN_A_ID = 7L;
    private static final Long TECHNICIAN_B_ID = 8L;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsTechnician(Long userId) {
        GarageUserPrincipal principal = new GarageUserPrincipal(
                userId, GARAGE_ID, "tech" + userId, "hash", "First", "Last",
                "tech@example.test", "9999999999", false, UserStatus.ACTIVE,
                Set.of("TECHNICIAN"), Set.of(), List.of()
        );
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private JobAssignment assignmentFor(Long userId) {
        User user = new User();
        user.setId(userId);
        JobAssignment a = new JobAssignment();
        a.setUser(user);
        a.setStatus(JobAssignmentStatus.IN_PROGRESS);
        return a;
    }

    private JobAssignmentResponse assignmentResponseFor(Long employeeId) {
        JobAssignmentResponse r = new JobAssignmentResponse();
        r.setEmployeeId(employeeId);
        return r;
    }

    @Test
    void technician_seesOnlyOwnAssignment_notOtherTechniciansAssignment() {

        Garage garage = new Garage();
        garage.setId(GARAGE_ID);

        JobCard jobCard = new JobCard();
        jobCard.setId(JOB_CARD_ID);
        jobCard.setGarage(garage);
        jobCard.setStatus(JobCardStatus.REPAIR_IN_PROGRESS);

        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard));

        // resolveViewer's technician-assignment check.
        when(jobAssignmentRepository.findByJobCardId(JOB_CARD_ID))
                .thenReturn(List.of(assignmentFor(TECHNICIAN_A_ID)));

        when(repairTaskService.getRepairTasks(JOB_CARD_ID)).thenReturn(List.of());

        // The projection's own assignments list contains BOTH technicians.
        when(jobAssignmentService.getAssignmentsByJobCard(JOB_CARD_ID))
                .thenReturn(List.of(
                        assignmentResponseFor(TECHNICIAN_A_ID),
                        assignmentResponseFor(TECHNICIAN_B_ID)));

        when(jobCardMapper.toResponse(jobCard)).thenReturn(
                com.garageos.modules.jobcard.dto.response.JobCardResponse.builder().build());

        authenticateAsTechnician(TECHNICIAN_A_ID);

        var view = service.getJobCardView(JOB_CARD_ID);

        assertThat(view.getAssignments()).hasSize(1);
        assertThat(view.getAssignments().get(0).getEmployeeId()).isEqualTo(TECHNICIAN_A_ID);
    }

    // ---- Technician repair actions must agree with the state machine ----

    /**
     * The projection used to list start_repair AND complete_repair
     * unconditionally, whatever state the Job Card was in. A technician
     * looking at a job at ESTIMATE_APPROVED was offered "Complete Repair"
     * on work that had not started - an action the transition table would
     * then refuse. The projection is what the client renders, so it must
     * agree with the state machine rather than contradict it.
     */
    private List<String> technicianActionsFor(JobCardStatus status) {

        Garage garage = new Garage();
        garage.setId(GARAGE_ID);

        JobCard jobCard = new JobCard();
        jobCard.setId(JOB_CARD_ID);
        jobCard.setGarage(garage);
        jobCard.setStatus(status);

        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard));
        when(jobAssignmentRepository.findByJobCardId(JOB_CARD_ID))
                .thenReturn(List.of(assignmentFor(TECHNICIAN_A_ID)));
        when(repairTaskService.getRepairTasks(JOB_CARD_ID)).thenReturn(List.of());
        when(jobAssignmentService.getAssignmentsByJobCard(JOB_CARD_ID))
                .thenReturn(List.of(assignmentResponseFor(TECHNICIAN_A_ID)));
        when(jobCardMapper.toResponse(jobCard)).thenReturn(
                com.garageos.modules.jobcard.dto.response.JobCardResponse.builder().build());

        authenticateAsTechnician(TECHNICIAN_A_ID);

        return service.getJobCardView(JOB_CARD_ID).getAllowedActions();
    }

    /**
     * ESTIMATE_APPROVED and WAITING_FOR_APPROVAL cannot legally reach
     * REPAIR_IN_PROGRESS - startRepair()'s target - so offering
     * "Start Repair" there would be an action the validator refuses.
     */
    @Test
    void technician_beforeRepairIsPending_isOfferedNoStartAction() {

        for (JobCardStatus status : List.of(
                JobCardStatus.WAITING_FOR_APPROVAL,
                JobCardStatus.ESTIMATE_APPROVED)) {

            List<String> actions = technicianActionsFor(status);

            assertThat(actions)
                    .as("%s cannot legally start repair", status)
                    .doesNotContain("start_repair", "complete_repair");
        }
    }

    @Test
    void technician_onRepairPending_isOfferedStartRepairOnly() {

        List<String> actions = technicianActionsFor(JobCardStatus.REPAIR_PENDING);

        assertThat(actions).contains("start_repair");
        assertThat(actions).doesNotContain("complete_repair");
    }

    @Test
    void technician_onRepairInProgress_isOfferedCompleteRepairOnly() {

        List<String> actions = technicianActionsFor(JobCardStatus.REPAIR_IN_PROGRESS);

        assertThat(actions).contains("complete_repair");
        assertThat(actions).doesNotContain("start_repair");
    }

    @Test
    void technician_neverSeesStartAndCompleteAtTheSameTime() {

        // The exact defect from the screenshot.
        for (JobCardStatus status : JobCardStatus.values()) {

            List<String> actions = technicianActionsFor(status);

            assertThat(actions.contains("start_repair")
                    && actions.contains("complete_repair"))
                    .as("%s offered both start_repair and complete_repair", status)
                    .isFalse();
        }
    }

    @Test
    void technician_afterRepairIsFinished_isOfferedNoRepairAction() {

        // Quality check, invoicing and delivery are not a technician's to
        // move. Offering nothing is the truthful answer.
        for (JobCardStatus status : List.of(
                JobCardStatus.REPAIR_COMPLETED,
                JobCardStatus.READY_FOR_INVOICE,
                JobCardStatus.INVOICE_GENERATED,
                JobCardStatus.READY_FOR_DELIVERY,
                JobCardStatus.DELIVERED,
                JobCardStatus.CLOSED,
                JobCardStatus.CANCELLED)) {

            List<String> actions = technicianActionsFor(status);

            assertThat(actions)
                    .as("%s must offer a technician no repair action", status)
                    .doesNotContain("start_repair", "complete_repair");
        }
    }

    @Test
    void technician_beforeTheEstimateIsApproved_isOfferedNoRepairAction() {

        for (JobCardStatus status : List.of(
                JobCardStatus.OPEN,
                JobCardStatus.INSPECTION_PENDING,
                JobCardStatus.INSPECTION_COMPLETED,
                JobCardStatus.ESTIMATE_PENDING)) {

            List<String> actions = technicianActionsFor(status);

            assertThat(actions)
                    .as("%s must offer a technician no repair action", status)
                    .doesNotContain("start_repair", "complete_repair");
        }
    }

    @Test
    void technician_alwaysRetainsViewAccess() {

        assertThat(technicianActionsFor(JobCardStatus.CLOSED)).contains("view");
    }

    /**
     * The projection must never offer an action the state machine would
     * refuse. This asserts that directly, against the real validator
     * rather than a second copy of its rules, so the two cannot drift
     * apart again.
     */
    @Test
    void everyOfferedRepairAction_isALegalTransition() {

        JobCardStatusValidator validator = new JobCardStatusValidator();

        for (JobCardStatus status : JobCardStatus.values()) {

            List<String> actions = technicianActionsFor(status);

            if (actions.contains("start_repair")) {
                assertThatCode(() -> validator.validate(
                        status, JobCardStatus.REPAIR_IN_PROGRESS))
                        .as("%s offered start_repair", status)
                        .doesNotThrowAnyException();
            }

            if (actions.contains("complete_repair")) {
                assertThatCode(() -> validator.validate(
                        status, JobCardStatus.REPAIR_COMPLETED))
                        .as("%s offered complete_repair", status)
                        .doesNotThrowAnyException();
            }
        }
    }
}
