package com.garageos.modules.jobassignment.service.impl;

import com.garageos.core.enums.JobAssignmentStatus;
import com.garageos.core.enums.JobAssignmentType;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.estimateitem.repository.EstimateItemRepository;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.jobassignment.dto.request.CompleteJobRequest;
import com.garageos.modules.jobassignment.dto.request.ReassignJobRequest;
import com.garageos.modules.jobassignment.dto.response.JobAssignmentResponse;
import com.garageos.modules.jobassignment.entity.JobAssignment;
import com.garageos.modules.jobassignment.mapper.JobAssignmentMapper;
import com.garageos.modules.jobassignment.repository.JobAssignmentRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.repairtask.entity.RepairTask;
import com.garageos.modules.repairtask.repository.RepairTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Reassignment must preserve history (old JobAssignment -> CANCELLED, a
 * new JobAssignment created) rather than mutating the existing row in
 * place, and must re-point the linked RepairTask at the new assignment.
 */
@ExtendWith(MockitoExtension.class)
class JobAssignmentServiceImplTest {

    @Mock private JobAssignmentRepository jobAssignmentRepository;
    @Mock private JobAssignmentMapper jobAssignmentMapper;
    @Mock private JobCardRepository jobCardRepository;
    @Mock private EstimateItemRepository estimateItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private GarageRepository garageRepository;
    @Mock private RepairTaskRepository repairTaskRepository;

    @InjectMocks
    private JobAssignmentServiceImpl service;

    private static final Long OLD_ASSIGNMENT_ID = 900L;
    private static final Long ESTIMATE_ITEM_ID = 700L;
    private static final Long OLD_TECHNICIAN_ID = 7L;
    private static final Long NEW_TECHNICIAN_ID = 8L;
    private static final Long GARAGE_ID = 10L;

    private JobAssignment oldAssignment() {
        Garage garage = new Garage();
        garage.setId(GARAGE_ID);

        JobCard jobCard = new JobCard();
        jobCard.setId(500L);

        EstimateItem item = new EstimateItem();
        item.setId(ESTIMATE_ITEM_ID);

        User oldUser = new User();
        oldUser.setId(OLD_TECHNICIAN_ID);

        JobAssignment assignment = new JobAssignment();
        assignment.setId(OLD_ASSIGNMENT_ID);
        assignment.setGarage(garage);
        assignment.setJobCard(jobCard);
        assignment.setEstimateItem(item);
        assignment.setUser(oldUser);
        assignment.setAssignmentType(JobAssignmentType.TECHNICIAN);
        assignment.setStatus(JobAssignmentStatus.IN_PROGRESS);
        return assignment;
    }

    @Test
    void reassign_cancelsOldAssignment_createsNewOne_andRelinksRepairTask() {

        JobAssignment old = oldAssignment();

        when(jobAssignmentRepository.findById(OLD_ASSIGNMENT_ID))
                .thenReturn(Optional.of(old));

        User newUser = new User();
        newUser.setId(NEW_TECHNICIAN_ID);
        when(userRepository.findById(NEW_TECHNICIAN_ID)).thenReturn(Optional.of(newUser));

        // First save() call cancels the old row; second save() persists the new row.
        when(jobAssignmentRepository.save(any(JobAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RepairTask task = RepairTask.builder().build();
        task.setId(600L);
        when(repairTaskRepository.findByEstimateItemId(ESTIMATE_ITEM_ID))
                .thenReturn(Optional.of(task));

        when(jobAssignmentMapper.toResponse(any(JobAssignment.class)))
                .thenReturn(new JobAssignmentResponse());

        ReassignJobRequest request = new ReassignJobRequest();
        request.setEmployeeId(NEW_TECHNICIAN_ID);
        request.setRemarks("reassigned");

        service.reassignJob(OLD_ASSIGNMENT_ID, request);

        assertThat(old.getStatus()).isEqualTo(JobAssignmentStatus.CANCELLED);

        ArgumentCaptor<JobAssignment> savedCaptor = ArgumentCaptor.forClass(JobAssignment.class);
        verify(jobAssignmentRepository, times(2)).save(savedCaptor.capture());

        JobAssignment newAssignment = savedCaptor.getAllValues().get(1);
        assertThat(newAssignment).isNotSameAs(old);
        assertThat(newAssignment.getUser().getId()).isEqualTo(NEW_TECHNICIAN_ID);
        assertThat(newAssignment.getStatus()).isEqualTo(JobAssignmentStatus.ASSIGNED);
        assertThat(newAssignment.getEstimateItem().getId()).isEqualTo(ESTIMATE_ITEM_ID);

        assertThat(task.getJobAssignment()).isSameAs(newAssignment);
        verify(repairTaskRepository).save(task);
    }

    /**
     * Fix 3 (audit Finding L1) regression coverage: JobAssignment
     * completion alone must never complete the JobCard — only
     * RepairTaskServiceImpl.completeRepair's aggregate check does that.
     * completeJob() has no JobCardRepository interaction at all.
     */
    @Test
    void completingJobAssignment_neverTouchesJobCard() {

        JobAssignment assignment = oldAssignment();
        assignment.setStatus(JobAssignmentStatus.IN_PROGRESS);

        when(jobAssignmentRepository.findById(OLD_ASSIGNMENT_ID))
                .thenReturn(Optional.of(assignment));
        when(jobAssignmentRepository.save(any(JobAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jobAssignmentMapper.toResponse(any(JobAssignment.class)))
                .thenReturn(new JobAssignmentResponse());

        CompleteJobRequest request = new CompleteJobRequest();
        request.setActualHours(2.5);

        service.completeJob(OLD_ASSIGNMENT_ID, request, OLD_TECHNICIAN_ID);

        assertThat(assignment.getStatus()).isEqualTo(JobAssignmentStatus.COMPLETED);
        verifyNoInteractions(jobCardRepository);
    }
}
