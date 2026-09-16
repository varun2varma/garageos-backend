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
}
