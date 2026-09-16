package com.garageos.modules.repairtask.service.impl;

import com.garageos.core.enums.JobAssignmentStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.RepairStatus;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.estimateitem.repository.EstimateItemRepository;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.jobassignment.dto.request.AssignJobRequest;
import com.garageos.modules.jobassignment.dto.request.ReassignJobRequest;
import com.garageos.modules.jobassignment.dto.response.JobAssignmentResponse;
import com.garageos.modules.jobassignment.entity.JobAssignment;
import com.garageos.modules.jobassignment.service.JobAssignmentService;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.jobcard.validator.JobCardStatusValidator;
import com.garageos.modules.qualitycheck.service.QualityCheckService;
import com.garageos.modules.repairtask.dto.request.AssignTechnicianRequest;
import com.garageos.modules.repairtask.entity.RepairTask;
import com.garageos.modules.repairtask.mapper.RepairTaskMapper;
import com.garageos.modules.repairtask.repository.RepairTaskRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers the canonical repair-completion aggregate trigger
 * (RepairTaskServiceImpl.completeRepair: last RepairTask completing sets
 * JobCard.REPAIR_COMPLETED and creates the QualityCheck) and the new
 * technician JobAssignment-based authorization on start/complete.
 */
@ExtendWith(MockitoExtension.class)
class RepairTaskServiceImplTest {

    @Mock private RepairTaskRepository repository;
    @Mock private EstimateItemRepository estimateItemRepository;
    @Mock private JobCardRepository jobCardRepository;
    @Mock private RepairTaskMapper mapper;
    @Mock private QualityCheckService qualityCheckService;
    @Mock private JobCardStatusValidator statusValidator;
    @Mock private JobAssignmentService jobAssignmentService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private RepairTaskServiceImpl service;

    private static final Long JOB_CARD_ID = 500L;
    private static final Long GARAGE_ID = 10L;
    private static final Long OTHER_GARAGE_ID = 20L;
    private static final Long TECHNICIAN_ID = 7L;
    private static final Long OTHER_TECHNICIAN_ID = 8L;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, Long garageId, String role) {
        GarageUserPrincipal principal = new GarageUserPrincipal(
                userId, garageId, "user" + userId, "hash", "First", "Last",
                "user@example.test", "9999999999", false, UserStatus.ACTIVE,
                Set.of(role), Set.of(), List.of()
        );
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private Garage garage(Long id) {
        Garage g = new Garage();
        g.setId(id);
        return g;
    }

    private User user(Long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private JobAssignment assignment(Long userId, Long garageId, JobAssignmentStatus status) {
        JobAssignment a = new JobAssignment();
        a.setId(900L);
        a.setUser(user(userId));
        a.setGarage(garage(garageId));
        a.setStatus(status);
        return a;
    }

    private RepairTask repairTask(RepairStatus status, JobAssignment assignment) {
        JobCard jobCard = new JobCard();
        jobCard.setId(JOB_CARD_ID);
        jobCard.setStatus(JobCardStatus.REPAIR_IN_PROGRESS);
        jobCard.setGarage(garage(GARAGE_ID));

        EstimateItem estimateItem = new EstimateItem();
        estimateItem.setId(700L);

        RepairTask task = RepairTask.builder()
                .jobCard(jobCard)
                .estimateItem(estimateItem)
                .status(status)
                .jobAssignment(assignment)
                .build();
        task.setId(600L);
        return task;
    }

    /**
     * Corrective fix for audit Finding L1: completing a Repair Task must
     * not be able to skip REPAIR_IN_PROGRESS. Previously all tasks could
     * complete while the JobCard was still at REPAIR_PENDING (the
     * JobCard-level "start repair" action was never required first),
     * silently advancing REPAIR_PENDING -> REPAIR_COMPLETED.
     */
    @Test
    void repairPending_allTasksComplete_completionIsRejected() {

        RepairTask task = repairTask(
                RepairStatus.ASSIGNED,
                assignment(TECHNICIAN_ID, GARAGE_ID, JobAssignmentStatus.IN_PROGRESS));
        task.getJobCard().setStatus(JobCardStatus.REPAIR_PENDING);

        when(repository.findById(600L)).thenReturn(Optional.of(task));

        authenticate(TECHNICIAN_ID, GARAGE_ID, "TECHNICIAN");

        assertThatThrownBy(() -> service.completeRepair(600L))
                .isInstanceOf(com.garageos.core.exception.BusinessException.class);

        assertThat(task.getStatus()).isEqualTo(RepairStatus.ASSIGNED);
        verify(repository, never()).save(any());
        verify(jobCardRepository, never()).save(any());
        verify(qualityCheckService, never()).createQualityCheck(any());
    }

    @Test
    void lastRepairTaskCompleting_movesJobCardToRepairCompleted_andCreatesQualityCheck() {

        RepairTask task = repairTask(
                RepairStatus.ASSIGNED,
                assignment(TECHNICIAN_ID, GARAGE_ID, JobAssignmentStatus.IN_PROGRESS));

        when(repository.findById(600L)).thenReturn(Optional.of(task));
        when(repository.save(any())).thenReturn(task);
        when(repository.countByJobCardId(JOB_CARD_ID)).thenReturn(1L);
        when(repository.countByJobCardIdAndStatus(JOB_CARD_ID, RepairStatus.COMPLETED)).thenReturn(1L);

        authenticate(TECHNICIAN_ID, GARAGE_ID, "TECHNICIAN");

        service.completeRepair(600L);

        assertThat(task.getJobCard().getStatus()).isEqualTo(JobCardStatus.REPAIR_COMPLETED);
        verify(qualityCheckService).createQualityCheck(task.getJobCard());
        verify(jobCardRepository).save(task.getJobCard());
    }

    @Test
    void notLastRepairTask_doesNotAdvanceJobCard() {

        RepairTask task = repairTask(
                RepairStatus.ASSIGNED,
                assignment(TECHNICIAN_ID, GARAGE_ID, JobAssignmentStatus.IN_PROGRESS));

        when(repository.findById(600L)).thenReturn(Optional.of(task));
        when(repository.save(any())).thenReturn(task);
        when(repository.countByJobCardId(JOB_CARD_ID)).thenReturn(3L);
        when(repository.countByJobCardIdAndStatus(JOB_CARD_ID, RepairStatus.COMPLETED)).thenReturn(1L);

        authenticate(TECHNICIAN_ID, GARAGE_ID, "TECHNICIAN");

        service.completeRepair(600L);

        assertThat(task.getJobCard().getStatus()).isEqualTo(JobCardStatus.REPAIR_IN_PROGRESS);
        verify(qualityCheckService, never()).createQualityCheck(any());
        verify(jobCardRepository, never()).save(any());
    }

    @Test
    void assignedTechnician_canCompleteTheirOwnRepairTask() {

        RepairTask task = repairTask(
                RepairStatus.ASSIGNED,
                assignment(TECHNICIAN_ID, GARAGE_ID, JobAssignmentStatus.IN_PROGRESS));

        when(repository.findById(600L)).thenReturn(Optional.of(task));
        when(repository.save(any())).thenReturn(task);
        when(repository.countByJobCardId(JOB_CARD_ID)).thenReturn(1L);
        when(repository.countByJobCardIdAndStatus(JOB_CARD_ID, RepairStatus.COMPLETED)).thenReturn(1L);
        when(mapper.toResponse(task)).thenReturn(
                com.garageos.modules.repairtask.dto.response.RepairTaskResponse.builder().build());

        authenticate(TECHNICIAN_ID, GARAGE_ID, "TECHNICIAN");

        assertThat(service.completeRepair(600L)).isNotNull();
    }

    @Test
    void differentTechnician_cannotCompleteSomeoneElsesRepairTask() {

        RepairTask task = repairTask(
                RepairStatus.ASSIGNED,
                assignment(TECHNICIAN_ID, GARAGE_ID, JobAssignmentStatus.IN_PROGRESS));

        when(repository.findById(600L)).thenReturn(Optional.of(task));

        authenticate(OTHER_TECHNICIAN_ID, GARAGE_ID, "TECHNICIAN");

        assertThatThrownBy(() -> service.completeRepair(600L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void technicianFromAnotherGarage_cannotCompleteRepairTask() {

        RepairTask task = repairTask(
                RepairStatus.ASSIGNED,
                assignment(TECHNICIAN_ID, GARAGE_ID, JobAssignmentStatus.IN_PROGRESS));

        when(repository.findById(600L)).thenReturn(Optional.of(task));

        authenticate(TECHNICIAN_ID, OTHER_GARAGE_ID, "TECHNICIAN");

        assertThatThrownBy(() -> service.completeRepair(600L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void technicianWithCancelledAssignment_cannotCompleteRepairTask() {

        RepairTask task = repairTask(
                RepairStatus.ASSIGNED,
                assignment(TECHNICIAN_ID, GARAGE_ID, JobAssignmentStatus.CANCELLED));

        when(repository.findById(600L)).thenReturn(Optional.of(task));

        authenticate(TECHNICIAN_ID, GARAGE_ID, "TECHNICIAN");

        assertThatThrownBy(() -> service.completeRepair(600L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void technicianWithNoAssignment_cannotStartRepairTask() {

        RepairTask task = repairTask(RepairStatus.ASSIGNED, null);

        when(repository.findById(600L)).thenReturn(Optional.of(task));

        authenticate(TECHNICIAN_ID, GARAGE_ID, "TECHNICIAN");

        assertThatThrownBy(() -> service.startRepair(600L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void manager_canCompleteAnyRepairTaskInGarage_regardlessOfAssignment() {

        RepairTask task = repairTask(RepairStatus.ASSIGNED, null);

        when(repository.findById(600L)).thenReturn(Optional.of(task));
        when(repository.save(any())).thenReturn(task);
        when(repository.countByJobCardId(JOB_CARD_ID)).thenReturn(1L);
        when(repository.countByJobCardIdAndStatus(JOB_CARD_ID, RepairStatus.COMPLETED)).thenReturn(1L);
        when(mapper.toResponse(task)).thenReturn(
                com.garageos.modules.repairtask.dto.response.RepairTaskResponse.builder().build());

        authenticate(999L, GARAGE_ID, "MANAGER");

        assertThat(service.completeRepair(600L)).isNotNull();
    }

    // ---- assignTechnician() legacy compatibility bridge (Fix 2 / T1) ----

    private User employee(Long id, Long garageId) {
        User u = new User();
        u.setId(id);
        u.setGarageId(garageId);
        return u;
    }

    private AssignTechnicianRequest requestFor(String name, Long employeeId) {
        return AssignTechnicianRequest.builder()
                .technicianName(name)
                .employeeId(employeeId)
                .build();
    }

    @Test
    void assignTechnician_withEmployeeId_createsJobAssignment_andLinksRepairTask() {

        RepairTask task = repairTask(RepairStatus.PENDING, null);

        when(repository.findById(600L)).thenReturn(Optional.of(task));
        when(repository.save(any())).thenReturn(task);
        when(userRepository.findById(TECHNICIAN_ID))
                .thenReturn(Optional.of(employee(TECHNICIAN_ID, GARAGE_ID)));
        when(mapper.toResponse(task)).thenReturn(
                com.garageos.modules.repairtask.dto.response.RepairTaskResponse.builder().build());

        authenticate(999L, GARAGE_ID, "MANAGER");

        service.assignTechnician(600L, requestFor("Real Name", TECHNICIAN_ID));

        assertThat(task.getTechnicianName()).isEqualTo("Real Name");
        assertThat(task.getStatus()).isEqualTo(RepairStatus.ASSIGNED);

        ArgumentCaptor<AssignJobRequest> captor = ArgumentCaptor.forClass(AssignJobRequest.class);
        verify(jobAssignmentService).assignJob(captor.capture());
        assertThat(captor.getValue().getEmployeeId()).isEqualTo(TECHNICIAN_ID);
        assertThat(captor.getValue().getEstimateItemId()).isEqualTo(700L);
    }

    @Test
    void assignTechnician_withoutEmployeeId_onlySetsDisplayName_noJobAssignmentCreated() {

        RepairTask task = repairTask(RepairStatus.PENDING, null);

        when(repository.findById(600L)).thenReturn(Optional.of(task));
        when(repository.save(any())).thenReturn(task);
        when(mapper.toResponse(task)).thenReturn(
                com.garageos.modules.repairtask.dto.response.RepairTaskResponse.builder().build());

        authenticate(999L, GARAGE_ID, "MANAGER");

        service.assignTechnician(600L, requestFor("Free Text Name", null));

        assertThat(task.getTechnicianName()).isEqualTo("Free Text Name");
        assertThat(task.getJobAssignment()).isNull();
        verifyNoInteractions(jobAssignmentService, userRepository);
    }

    @Test
    void assignTechnician_reassignment_cancelsOldAssignment_viaSharedMechanism() {

        RepairTask task = repairTask(
                RepairStatus.ASSIGNED,
                assignment(OTHER_TECHNICIAN_ID, GARAGE_ID, JobAssignmentStatus.ASSIGNED));
        task.getJobAssignment().setId(900L);

        when(repository.findById(600L)).thenReturn(Optional.of(task));
        when(repository.save(any())).thenReturn(task);
        when(userRepository.findById(TECHNICIAN_ID))
                .thenReturn(Optional.of(employee(TECHNICIAN_ID, GARAGE_ID)));
        when(mapper.toResponse(task)).thenReturn(
                com.garageos.modules.repairtask.dto.response.RepairTaskResponse.builder().build());

        authenticate(999L, GARAGE_ID, "MANAGER");

        service.assignTechnician(600L, requestFor("New Technician", TECHNICIAN_ID));

        ArgumentCaptor<ReassignJobRequest> captor = ArgumentCaptor.forClass(ReassignJobRequest.class);
        verify(jobAssignmentService).reassignJob(eq(900L), captor.capture());
        assertThat(captor.getValue().getEmployeeId()).isEqualTo(TECHNICIAN_ID);
        verify(jobAssignmentService, never()).assignJob(any());
    }

    @Test
    void assignTechnician_crossGarageEmployee_isRejected() {

        RepairTask task = repairTask(RepairStatus.PENDING, null);

        when(repository.findById(600L)).thenReturn(Optional.of(task));
        when(repository.save(any())).thenReturn(task);
        when(userRepository.findById(TECHNICIAN_ID))
                .thenReturn(Optional.of(employee(TECHNICIAN_ID, OTHER_GARAGE_ID)));

        authenticate(999L, GARAGE_ID, "MANAGER");

        assertThatThrownBy(() ->
                service.assignTechnician(600L, requestFor("Real Name", TECHNICIAN_ID)))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(jobAssignmentService);
    }

    /**
     * Explicit proof that an arbitrary technicianName string can never
     * authorize execution: the task below has technicianName set to the
     * exact same name a technician-role caller would present as their own,
     * but no JobAssignment link — the caller is still denied.
     */
    @Test
    void arbitraryTechnicianName_cannotAuthorizeExecution_withoutJobAssignment() {

        RepairTask task = repairTask(RepairStatus.ASSIGNED, null);
        task.setTechnicianName("First Last");

        when(repository.findById(600L)).thenReturn(Optional.of(task));

        authenticate(TECHNICIAN_ID, GARAGE_ID, "TECHNICIAN");

        assertThatThrownBy(() -> service.startRepair(600L))
                .isInstanceOf(BusinessException.class);
    }

    // ---- Cross-garage tenant authorization (Defect #5 corrective fix) ----
    // Confirmed live: a Garage B MANAGER was able to complete a Garage A
    // RepairTask (and trigger the JobCard->REPAIR_COMPLETED aggregate +
    // QualityCheck creation side effects) because the MANAGER/OWNER/
    // SERVICE_ADVISOR branch of authorizeRepairTaskAction() never checked
    // the caller's own garage against the RepairTask's JobCard garage.

    @Test
    void garageA_manager_completingGarageA_task_isAllowed() {

        RepairTask task = repairTask(RepairStatus.ASSIGNED,
                assignment(TECHNICIAN_ID, GARAGE_ID, JobAssignmentStatus.IN_PROGRESS));

        when(repository.findById(600L)).thenReturn(Optional.of(task));
        when(repository.save(any())).thenReturn(task);
        when(repository.countByJobCardId(JOB_CARD_ID)).thenReturn(1L);
        when(repository.countByJobCardIdAndStatus(JOB_CARD_ID, RepairStatus.COMPLETED)).thenReturn(1L);
        when(mapper.toResponse(task)).thenReturn(
                com.garageos.modules.repairtask.dto.response.RepairTaskResponse.builder().build());

        authenticate(999L, GARAGE_ID, "MANAGER");

        assertThat(service.completeRepair(600L)).isNotNull();
        assertThat(task.getStatus()).isEqualTo(RepairStatus.COMPLETED);
    }

    @Test
    void garageB_manager_completingGarageA_task_isDenied_withNoMutation() {

        RepairTask task = repairTask(RepairStatus.ASSIGNED,
                assignment(TECHNICIAN_ID, GARAGE_ID, JobAssignmentStatus.IN_PROGRESS));

        when(repository.findById(600L)).thenReturn(Optional.of(task));

        authenticate(999L, OTHER_GARAGE_ID, "MANAGER");

        assertThatThrownBy(() -> service.completeRepair(600L))
                .isInstanceOf(BusinessException.class);

        assertThat(task.getStatus()).isEqualTo(RepairStatus.ASSIGNED);
        verify(repository, never()).save(any());
        verify(jobCardRepository, never()).save(any());
        verify(qualityCheckService, never()).createQualityCheck(any());
    }

    @Test
    void garageA_owner_operatingGarageA_task_isAllowed() {

        RepairTask task = repairTask(RepairStatus.ASSIGNED,
                assignment(TECHNICIAN_ID, GARAGE_ID, JobAssignmentStatus.IN_PROGRESS));

        when(repository.findById(600L)).thenReturn(Optional.of(task));
        when(repository.save(any())).thenReturn(task);
        when(repository.countByJobCardId(JOB_CARD_ID)).thenReturn(1L);
        when(repository.countByJobCardIdAndStatus(JOB_CARD_ID, RepairStatus.COMPLETED)).thenReturn(1L);
        when(mapper.toResponse(task)).thenReturn(
                com.garageos.modules.repairtask.dto.response.RepairTaskResponse.builder().build());

        authenticate(999L, GARAGE_ID, "OWNER");

        assertThat(service.completeRepair(600L)).isNotNull();
    }

    @Test
    void garageB_owner_operatingGarageA_task_isDenied_withNoMutation() {

        RepairTask task = repairTask(RepairStatus.ASSIGNED,
                assignment(TECHNICIAN_ID, GARAGE_ID, JobAssignmentStatus.IN_PROGRESS));

        when(repository.findById(600L)).thenReturn(Optional.of(task));

        authenticate(999L, OTHER_GARAGE_ID, "OWNER");

        assertThatThrownBy(() -> service.startRepair(600L))
                .isInstanceOf(BusinessException.class);

        assertThat(task.getStatus()).isEqualTo(RepairStatus.ASSIGNED);
        verify(repository, never()).save(any());
    }

    @Test
    void garageA_serviceAdvisor_operatingGarageA_task_isAllowed() {

        RepairTask task = repairTask(RepairStatus.ASSIGNED,
                assignment(TECHNICIAN_ID, GARAGE_ID, JobAssignmentStatus.IN_PROGRESS));

        when(repository.findById(600L)).thenReturn(Optional.of(task));
        when(repository.save(any())).thenReturn(task);
        when(repository.countByJobCardId(JOB_CARD_ID)).thenReturn(1L);
        when(repository.countByJobCardIdAndStatus(JOB_CARD_ID, RepairStatus.COMPLETED)).thenReturn(1L);
        when(mapper.toResponse(task)).thenReturn(
                com.garageos.modules.repairtask.dto.response.RepairTaskResponse.builder().build());

        authenticate(999L, GARAGE_ID, "SERVICE_ADVISOR");

        assertThat(service.completeRepair(600L)).isNotNull();
    }

    @Test
    void garageB_serviceAdvisor_operatingGarageA_task_isDenied_withNoMutation() {

        RepairTask task = repairTask(RepairStatus.ASSIGNED,
                assignment(TECHNICIAN_ID, GARAGE_ID, JobAssignmentStatus.IN_PROGRESS));

        when(repository.findById(600L)).thenReturn(Optional.of(task));

        authenticate(999L, OTHER_GARAGE_ID, "SERVICE_ADVISOR");

        assertThatThrownBy(() -> service.completeRepair(600L))
                .isInstanceOf(BusinessException.class);

        assertThat(task.getStatus()).isEqualTo(RepairStatus.ASSIGNED);
        verify(repository, never()).save(any());
        verify(jobCardRepository, never()).save(any());
        verify(qualityCheckService, never()).createQualityCheck(any());
    }

    @Test
    void garageB_manager_assigningTechnicianToGarageA_task_isDenied_withNoMutation() {

        RepairTask task = repairTask(RepairStatus.PENDING, null);

        when(repository.findById(600L)).thenReturn(Optional.of(task));

        authenticate(999L, OTHER_GARAGE_ID, "MANAGER");

        assertThatThrownBy(() ->
                service.assignTechnician(600L, requestFor("Real Name", TECHNICIAN_ID)))
                .isInstanceOf(BusinessException.class);

        assertThat(task.getTechnicianName()).isNull();
        assertThat(task.getStatus()).isEqualTo(RepairStatus.PENDING);
        verify(repository, never()).save(any());
        verifyNoInteractions(jobAssignmentService, userRepository);
    }

    @Test
    void garageA_manager_assigningGarageA_technician_isAllowed() {

        RepairTask task = repairTask(RepairStatus.PENDING, null);

        when(repository.findById(600L)).thenReturn(Optional.of(task));
        when(repository.save(any())).thenReturn(task);
        when(userRepository.findById(TECHNICIAN_ID))
                .thenReturn(Optional.of(employee(TECHNICIAN_ID, GARAGE_ID)));
        when(mapper.toResponse(task)).thenReturn(
                com.garageos.modules.repairtask.dto.response.RepairTaskResponse.builder().build());

        authenticate(999L, GARAGE_ID, "MANAGER");

        service.assignTechnician(600L, requestFor("Real Name", TECHNICIAN_ID));

        assertThat(task.getTechnicianName()).isEqualTo("Real Name");
        verify(jobAssignmentService).assignJob(any());
    }

    @Test
    void technicianFromAnotherGarage_isDenied_evenIfAssignmentSomehowExists() {

        RepairTask task = repairTask(RepairStatus.ASSIGNED,
                assignment(TECHNICIAN_ID, GARAGE_ID, JobAssignmentStatus.IN_PROGRESS));

        when(repository.findById(600L)).thenReturn(Optional.of(task));

        // Same user id as the assignment, but authenticated against a
        // different garage than both the assignment and the JobCard.
        authenticate(TECHNICIAN_ID, OTHER_GARAGE_ID, "TECHNICIAN");

        assertThatThrownBy(() -> service.startRepair(600L))
                .isInstanceOf(BusinessException.class);

        assertThat(task.getStatus()).isEqualTo(RepairStatus.ASSIGNED);
        verify(repository, never()).save(any());
    }
}
