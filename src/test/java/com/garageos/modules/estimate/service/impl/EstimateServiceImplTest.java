package com.garageos.modules.estimate.service.impl;

import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.estimate.mapper.EstimateMapper;
import com.garageos.modules.estimate.repository.EstimateRepository;
import com.garageos.modules.estimateitem.repository.EstimateItemRepository;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.inspection.repository.InspectionRepository;
import com.garageos.modules.inspectionfinding.repository.InspectionFindingRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.jobcard.validator.JobCardStatusValidator;
import com.garageos.modules.repairtask.service.RepairTaskService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the P0 hotfix on EstimateServiceImpl.approveEstimate/
 * rejectEstimate(Long id) — the methods backing PUT /estimates/{id}/approve
 * and /reject, the exact endpoints the customer portal (legacy JS and
 * Flutter) actually call. Previously these performed no ownership or role
 * check at all: any authenticated principal — any role, any garage, any
 * customer — could approve or reject any estimate by ID.
 *
 * No live client UI calls this endpoint pair as an employee (confirmed by
 * source inspection of both the legacy JS and Flutter clients), so per
 * product decision this hotfix scopes access to CUSTOMER-role callers who
 * own the estimate's job card only; every other caller sees the same
 * not-found response an absent estimate ID would produce.
 */
@ExtendWith(MockitoExtension.class)
class EstimateServiceImplTest {

    @Mock private EstimateRepository estimateRepository;
    @Mock private JobCardRepository jobCardRepository;
    @Mock private EstimateMapper estimateMapper;
    @Mock private InspectionRepository inspectionRepository;
    @Mock private EstimateItemRepository estimateItemRepository;
    @Mock private InspectionFindingRepository inspectionFindingRepository;
    @Mock private RepairTaskService repairTaskService;
    @Mock private CustomerRepository customerRepository;
    @Mock private JobCardStatusValidator statusValidator;

    @InjectMocks
    private EstimateServiceImpl service;

    private static final Long ESTIMATE_ID = 300L;
    private static final Long OWNING_CUSTOMER_ID = 1L;
    private static final Long OTHER_CUSTOMER_ID = 2L;
    private static final Long GARAGE_A_ID = 10L;
    private static final Long GARAGE_B_ID = 20L;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsCustomer(String mobile) {
        GarageUserPrincipal principal = new GarageUserPrincipal(
                5L, null, "cust", "hash", "First", "Last",
                "cust@example.test", mobile, false, UserStatus.ACTIVE,
                Set.of("CUSTOMER"), Set.of(), List.of()
        );
        authenticate(principal);
    }

    private void authenticateAsEmployee(Long garageId, String role) {
        GarageUserPrincipal principal = new GarageUserPrincipal(
                7L, garageId, "emp", "hash", "First", "Last",
                "emp@example.test", "9999999999", false, UserStatus.ACTIVE,
                Set.of(role), Set.of(), List.of()
        );
        authenticate(principal);
    }

    private void authenticate(GarageUserPrincipal principal) {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private Customer customer(Long id, String mobile) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setMobileNumber(mobile);
        return customer;
    }

    private Estimate estimateOwnedBy(Customer owner, Long garageId) {
        Garage garage = new Garage();
        garage.setId(garageId);

        JobCard jobCard = new JobCard();
        jobCard.setId(500L);
        jobCard.setCustomer(owner);
        jobCard.setGarage(garage);

        Estimate estimate = new Estimate();
        estimate.setId(ESTIMATE_ID);
        estimate.setJobCard(jobCard);
        estimate.setStatus(EstimateStatus.WAITING_FOR_APPROVAL);
        return estimate;
    }

    // ---- APPROVE ----

    @Test
    void customer_canApproveTheirOwnEstimate() {
        Customer owner = customer(OWNING_CUSTOMER_ID, "9000000001");
        when(customerRepository.findByMobileNumber("9000000001")).thenReturn(Optional.of(owner));

        Estimate estimate = estimateOwnedBy(owner, GARAGE_A_ID);
        when(estimateRepository.findById(ESTIMATE_ID)).thenReturn(Optional.of(estimate));
        when(estimateRepository.save(estimate)).thenReturn(estimate);
        when(estimateMapper.toResponse(estimate))
                .thenReturn(com.garageos.modules.estimate.dto.response.EstimateResponse.builder().build());

        authenticateAsCustomer("9000000001");

        assertThat(service.approveEstimate(ESTIMATE_ID)).isNotNull();

        // Security semantics (ownership) are unchanged by this task; the
        // JobCard target status now reflects the canonical GarageST
        // JobCard Architecture v1 lifecycle (REPAIR_PENDING directly,
        // never the legacy ESTIMATE_APPROVED state).
        assertThat(estimate.getStatus()).isEqualTo(EstimateStatus.APPROVED);
        assertThat(estimate.getJobCard().getStatus()).isEqualTo(JobCardStatus.REPAIR_PENDING);
        verify(repairTaskService).createRepairTasks(estimate);
    }

    @Test
    void customerA_cannotApproveCustomerBsEstimate() {
        Customer requester = customer(OTHER_CUSTOMER_ID, "9000000002");
        when(customerRepository.findByMobileNumber("9000000002")).thenReturn(Optional.of(requester));

        Customer owner = customer(OWNING_CUSTOMER_ID, "9000000001");
        Estimate estimate = estimateOwnedBy(owner, GARAGE_A_ID);
        when(estimateRepository.findById(ESTIMATE_ID)).thenReturn(Optional.of(estimate));

        authenticateAsCustomer("9000000002");

        assertThatThrownBy(() -> service.approveEstimate(ESTIMATE_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(estimate.getStatus()).isEqualTo(EstimateStatus.WAITING_FOR_APPROVAL);
        verify(repairTaskService, never()).createRepairTasks(estimate);
        verify(estimateRepository, never()).save(estimate);
    }

    @Test
    void customer_cannotApproveEstimateBelongingToAnotherGarageAndCustomer() {
        Customer requester = customer(OWNING_CUSTOMER_ID, "9000000001");
        when(customerRepository.findByMobileNumber("9000000001")).thenReturn(Optional.of(requester));

        Customer owner = customer(OTHER_CUSTOMER_ID, "9000000002");
        Estimate estimate = estimateOwnedBy(owner, GARAGE_B_ID);
        when(estimateRepository.findById(ESTIMATE_ID)).thenReturn(Optional.of(estimate));

        authenticateAsCustomer("9000000001");

        assertThatThrownBy(() -> service.approveEstimate(ESTIMATE_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void employeeInSameGarage_cannotApprove_viaThisEndpoint() {
        Customer owner = customer(OWNING_CUSTOMER_ID, "9000000001");
        Estimate estimate = estimateOwnedBy(owner, GARAGE_A_ID);
        when(estimateRepository.findById(ESTIMATE_ID)).thenReturn(Optional.of(estimate));

        authenticateAsEmployee(GARAGE_A_ID, "MANAGER");

        assertThatThrownBy(() -> service.approveEstimate(ESTIMATE_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(estimate.getStatus()).isEqualTo(EstimateStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void employeeInOtherGarage_cannotApprove() {
        Customer owner = customer(OWNING_CUSTOMER_ID, "9000000001");
        Estimate estimate = estimateOwnedBy(owner, GARAGE_A_ID);
        when(estimateRepository.findById(ESTIMATE_ID)).thenReturn(Optional.of(estimate));

        authenticateAsEmployee(GARAGE_B_ID, "MANAGER");

        assertThatThrownBy(() -> service.approveEstimate(ESTIMATE_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- REJECT ----

    @Test
    void customer_canRejectTheirOwnEstimate() {
        Customer owner = customer(OWNING_CUSTOMER_ID, "9000000001");
        when(customerRepository.findByMobileNumber("9000000001")).thenReturn(Optional.of(owner));

        Estimate estimate = estimateOwnedBy(owner, GARAGE_A_ID);
        when(estimateRepository.findById(ESTIMATE_ID)).thenReturn(Optional.of(estimate));
        when(estimateRepository.save(estimate)).thenReturn(estimate);
        when(estimateMapper.toResponse(estimate))
                .thenReturn(com.garageos.modules.estimate.dto.response.EstimateResponse.builder().build());

        authenticateAsCustomer("9000000001");

        assertThat(service.rejectEstimate(ESTIMATE_ID)).isNotNull();
        assertThat(estimate.getStatus()).isEqualTo(EstimateStatus.REJECTED);
    }

    @Test
    void customerA_cannotRejectCustomerBsEstimate() {
        Customer requester = customer(OTHER_CUSTOMER_ID, "9000000002");
        when(customerRepository.findByMobileNumber("9000000002")).thenReturn(Optional.of(requester));

        Customer owner = customer(OWNING_CUSTOMER_ID, "9000000001");
        Estimate estimate = estimateOwnedBy(owner, GARAGE_A_ID);
        when(estimateRepository.findById(ESTIMATE_ID)).thenReturn(Optional.of(estimate));

        authenticateAsCustomer("9000000002");

        assertThatThrownBy(() -> service.rejectEstimate(ESTIMATE_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(estimate.getStatus()).isEqualTo(EstimateStatus.WAITING_FOR_APPROVAL);
    }

    // ---- EMPLOYEE WORKFLOW APPROVAL (approveEstimate(String)) ----
    // Corrective fix for audit Finding E1: this overload previously had
    // no role/garage check at all — any authenticated principal could
    // approve any estimate. Locked decision: MANAGER-only, same-garage.

    private static final String JOB_CARD_NUMBER = "JC-2026-000500";

    private JobCard jobCardInGarage(Long garageId) {
        Garage garage = new Garage();
        garage.setId(garageId);
        JobCard jobCard = new JobCard();
        jobCard.setId(500L);
        jobCard.setJobCardNumber(JOB_CARD_NUMBER);
        jobCard.setGarage(garage);
        jobCard.setStatus(JobCardStatus.WAITING_FOR_APPROVAL);
        return jobCard;
    }

    private Estimate estimateFor(JobCard jobCard) {
        Estimate estimate = new Estimate();
        estimate.setId(ESTIMATE_ID);
        estimate.setJobCard(jobCard);
        estimate.setStatus(EstimateStatus.WAITING_FOR_APPROVAL);
        return estimate;
    }

    @Test
    void manager_sameGarage_canApproveEmployeeWorkflowEstimate() {
        JobCard jobCard = jobCardInGarage(GARAGE_A_ID);
        when(jobCardRepository.findByJobCardNumber(JOB_CARD_NUMBER)).thenReturn(Optional.of(jobCard));

        Estimate estimate = estimateFor(jobCard);
        when(estimateRepository.findByJobCardId(jobCard.getId())).thenReturn(Optional.of(estimate));
        when(estimateRepository.save(estimate)).thenReturn(estimate);
        when(estimateMapper.toResponse(estimate))
                .thenReturn(com.garageos.modules.estimate.dto.response.EstimateResponse.builder().build());

        authenticateAsEmployee(GARAGE_A_ID, "MANAGER");

        assertThat(service.approveEstimate(JOB_CARD_NUMBER)).isNotNull();
        assertThat(estimate.getStatus()).isEqualTo(EstimateStatus.APPROVED);
        assertThat(jobCard.getStatus()).isEqualTo(JobCardStatus.REPAIR_PENDING);
    }

    @Test
    void manager_differentGarage_deniedOnEmployeeWorkflowEstimate() {
        JobCard jobCard = jobCardInGarage(GARAGE_A_ID);
        when(jobCardRepository.findByJobCardNumber(JOB_CARD_NUMBER)).thenReturn(Optional.of(jobCard));

        authenticateAsEmployee(GARAGE_B_ID, "MANAGER");

        assertThatThrownBy(() -> service.approveEstimate(JOB_CARD_NUMBER))
                .isInstanceOf(com.garageos.core.exception.BusinessException.class);

        assertThat(jobCard.getStatus()).isEqualTo(JobCardStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void serviceAdvisor_deniedOnEmployeeWorkflowEstimate() {
        JobCard jobCard = jobCardInGarage(GARAGE_A_ID);
        when(jobCardRepository.findByJobCardNumber(JOB_CARD_NUMBER)).thenReturn(Optional.of(jobCard));

        authenticateAsEmployee(GARAGE_A_ID, "SERVICE_ADVISOR");

        assertThatThrownBy(() -> service.approveEstimate(JOB_CARD_NUMBER))
                .isInstanceOf(com.garageos.core.exception.BusinessException.class);
    }

    @Test
    void technician_deniedOnEmployeeWorkflowEstimate() {
        JobCard jobCard = jobCardInGarage(GARAGE_A_ID);
        when(jobCardRepository.findByJobCardNumber(JOB_CARD_NUMBER)).thenReturn(Optional.of(jobCard));

        authenticateAsEmployee(GARAGE_A_ID, "TECHNICIAN");

        assertThatThrownBy(() -> service.approveEstimate(JOB_CARD_NUMBER))
                .isInstanceOf(com.garageos.core.exception.BusinessException.class);
    }

    @Test
    void customer_deniedOnEmployeeWorkflowEstimate() {
        JobCard jobCard = jobCardInGarage(GARAGE_A_ID);
        when(jobCardRepository.findByJobCardNumber(JOB_CARD_NUMBER)).thenReturn(Optional.of(jobCard));

        authenticateAsCustomer("9000000001");

        assertThatThrownBy(() -> service.approveEstimate(JOB_CARD_NUMBER))
                .isInstanceOf(com.garageos.core.exception.BusinessException.class);
    }

    /**
     * Locked decision explicitly confirmed: OWNER is NOT granted
     * employee estimate-approval authority, even though Owner and
     * Manager are equivalent for other operational JobCard actions.
     * This is a deliberate distinction, not an oversight — see
     * EstimateServiceImpl.authorizeEmployeeEstimateApproval.
     */
    @Test
    void owner_deniedOnEmployeeWorkflowEstimate_perLockedManagerOnlyRule() {
        JobCard jobCard = jobCardInGarage(GARAGE_A_ID);
        when(jobCardRepository.findByJobCardNumber(JOB_CARD_NUMBER)).thenReturn(Optional.of(jobCard));

        authenticateAsEmployee(GARAGE_A_ID, "OWNER");

        assertThatThrownBy(() -> service.approveEstimate(JOB_CARD_NUMBER))
                .isInstanceOf(com.garageos.core.exception.BusinessException.class);
    }
}
