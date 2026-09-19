package com.garageos.modules.jobassignment.service.impl;

import com.garageos.core.enums.JobAssignmentStatus;
import com.garageos.core.enums.JobAssignmentType;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.RepairStatus;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.estimateitem.repository.EstimateItemRepository;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.jobassignment.dto.request.AssignJobRequest;
import com.garageos.modules.jobassignment.dto.request.CompleteJobRequest;
import com.garageos.modules.jobassignment.dto.request.ReassignJobRequest;
import com.garageos.modules.jobassignment.dto.request.StartJobRequest;
import com.garageos.modules.jobassignment.dto.response.JobAssignmentResponse;
import com.garageos.modules.jobassignment.dto.response.MyAssignmentResponse;
import com.garageos.modules.jobassignment.entity.JobAssignment;
import com.garageos.modules.jobassignment.mapper.JobAssignmentMapper;
import com.garageos.modules.jobassignment.repository.JobAssignmentRepository;
import com.garageos.modules.jobassignment.service.JobAssignmentService;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.jobcard.validator.JobCardStatusValidator;
import com.garageos.modules.qualitycheck.service.QualityCheckService;
import com.garageos.modules.repairtask.entity.RepairTask;
import com.garageos.modules.repairtask.repository.RepairTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class JobAssignmentServiceImpl implements JobAssignmentService {

    private final JobAssignmentRepository jobAssignmentRepository;

    private final JobAssignmentMapper jobAssignmentMapper;

    private final JobCardRepository jobCardRepository;

    private final EstimateItemRepository estimateItemRepository;

    private final UserRepository userRepository;

    private final GarageRepository garageRepository;

    private final RepairTaskRepository repairTaskRepository;

    private final JobCardStatusValidator statusValidator;

    private final QualityCheckService qualityCheckService;

    @Override
    @Transactional
    public JobAssignmentResponse assignJob(
            AssignJobRequest request) {

        JobCard jobCard =
                jobCardRepository.findById(
                        request.getJobCardId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found : "
                                        + request.getJobCardId()
                        )
                );

        EstimateItem estimateItem = null;

        RepairTask repairTask = null;

        if (request.getAssignmentType()
                == JobAssignmentType.TECHNICIAN) {

            if (request.getRepairTaskId() == null) {
                throw new IllegalArgumentException(
                        "Repair task is required for technician assignment."
                );
            }

            repairTask =
                    repairTaskRepository.findById(
                            request.getRepairTaskId()
                    ).orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Repair Task not found : "
                                            + request.getRepairTaskId()
                            ));
        }

        User user =
                userRepository.findById(
                        request.getEmployeeId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found : "
                                        + request.getEmployeeId()
                        )
                );

        Garage garage =
                garageRepository.findById(
                        user.getGarageId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Garage not found"
                        )
                );

        JobAssignment assignment =
                new JobAssignment();

        assignment.setGarage(garage);

        assignment.setJobCard(jobCard);

        assignment.setEstimateItem(estimateItem);

        assignment.setRepairTask(repairTask);

        assignment.setUser(user);

        assignment.setAssignmentType(
                request.getAssignmentType()
        );

        assignment.setAssignedAt(
                LocalDateTime.now()
        );

        assignment.setEstimatedHours(
                request.getEstimatedHours()
        );

        assignment.setRemarks(
                request.getRemarks()
        );

        assignment.setStatus(
                JobAssignmentStatus.ASSIGNED
        );

        assignment =
                jobAssignmentRepository.save(
                        assignment
                );

        linkRepairTaskToAssignment(assignment);

        return jobAssignmentMapper.toResponse(
                assignment
        );
    }

    /**
     * Links the RepairTask to the current JobAssignment.
     *
     * For technician assignments, creating the assignment is the
     * domain event that moves a PENDING RepairTask to ASSIGNED.
     */
    private void linkRepairTaskToAssignment(
            JobAssignment assignment) {

        if (assignment.getRepairTask() == null) {
            return;
        }

        RepairTask task =
                assignment.getRepairTask();

        task.setJobAssignment(assignment);

        if (assignment.getAssignmentType()
                == JobAssignmentType.TECHNICIAN
                && task.getStatus() == RepairStatus.PENDING) {

            task.setStatus(
                    RepairStatus.ASSIGNED
            );

            task.setAssignedAt(
                    LocalDateTime.now()
            );
        }

        repairTaskRepository.save(task);
    }

    @Override
    @Transactional
    public JobAssignmentResponse acceptJob(
            Long assignmentId,
            Long userId) {

        JobAssignment assignment =
                getAssignmentForUserOrThrow(
                        assignmentId,
                        userId
                );

        if (assignment.getStatus()
                != JobAssignmentStatus.ASSIGNED) {

            throw new IllegalStateException(
                    "Job cannot be taken in current status."
            );
        }

        /*
         * RepairTask is already ASSIGNED when the technician
         * assignment is created.
         *
         * Accepting the job only changes the JobAssignment
         * lifecycle. RepairTask remains ASSIGNED until Start.
         */
        assignment.setStatus(
                JobAssignmentStatus.ACCEPTED
        );

        assignment.setAcceptedAt(
                LocalDateTime.now()
        );

        assignment =
                jobAssignmentRepository.save(
                        assignment
                );

        return jobAssignmentMapper.toResponse(
                assignment
        );
    }

    @Override
    @Transactional
    public JobAssignmentResponse startJob(
            Long assignmentId,
            StartJobRequest request,
            Long userId) {

        JobAssignment assignment =
                getAssignmentForUserOrThrow(
                        assignmentId,
                        userId
                );

        if (assignment.getStatus()
                != JobAssignmentStatus.ACCEPTED) {

            throw new IllegalStateException(
                    "Job must be taken before starting."
            );
        }

        RepairTask task =
                assignment.getRepairTask();

        /*
         * Technician assignments must have a RepairTask.
         */
        if (assignment.getAssignmentType()
                == JobAssignmentType.TECHNICIAN) {

            if (task == null) {
                throw new IllegalStateException(
                        "Technician assignment has no repair task."
                );
            }

            if (task.getStatus()
                    != RepairStatus.ASSIGNED) {

                throw new IllegalStateException(
                        "Repair Task must be ASSIGNED before starting."
                );
            }
        }

        /*
         * Start the JobAssignment.
         */
        assignment.setStatus(
                JobAssignmentStatus.IN_PROGRESS
        );

        assignment.setStartedAt(
                LocalDateTime.now()
        );

        if (request != null) {
            assignment.setRemarks(
                    request.getRemarks()
            );
        }

        /*
         * Start the RepairTask and JobCard as part of
         * the same transaction.
         */
        if (assignment.getAssignmentType()
                == JobAssignmentType.TECHNICIAN) {

            task.setStatus(
                    RepairStatus.IN_PROGRESS
            );

            task.setStartedAt(
                    LocalDateTime.now()
            );

            JobCard jobCard =
                    task.getJobCard();

            if (jobCard != null
                    && jobCard.getStatus()
                    == JobCardStatus.REPAIR_PENDING) {

                jobCard.setStatus(
                        JobCardStatus.REPAIR_IN_PROGRESS
                );

                jobCardRepository.save(
                        jobCard
                );
            }

            repairTaskRepository.save(
                    task
            );
        }

        assignment =
                jobAssignmentRepository.save(
                        assignment
                );

        return jobAssignmentMapper.toResponse(
                assignment
        );
    }

    @Override
    @Transactional
    public JobAssignmentResponse completeJob(
            Long assignmentId,
            CompleteJobRequest request,
            Long userId) {

        JobAssignment assignment =
                getAssignmentForUserOrThrow(
                        assignmentId,
                        userId
                );

        if (assignment.getStatus()
                != JobAssignmentStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "Job must be started before completing."
            );
        }

        RepairTask task =
                assignment.getRepairTask();

        /*
         * Technician assignments must have a RepairTask.
         */
        if (assignment.getAssignmentType()
                == JobAssignmentType.TECHNICIAN) {

            if (task == null) {
                throw new IllegalStateException(
                        "Technician assignment has no repair task."
                );
            }

            if (task.getStatus()
                    != RepairStatus.IN_PROGRESS) {

                throw new IllegalStateException(
                        "Repair Task must be IN_PROGRESS before completion."
                );
            }
        }

        /*
         * Complete the JobAssignment.
         */
        assignment.setStatus(
                JobAssignmentStatus.COMPLETED
        );

        assignment.setCompletedAt(
                LocalDateTime.now()
        );

        if (request != null) {

            assignment.setActualHours(
                    request.getActualHours()
            );

            assignment.setRemarks(
                    request.getRemarks()
            );
        }

        /*
         * Complete the RepairTask as part of the same transaction.
         */
        if (assignment.getAssignmentType()
                == JobAssignmentType.TECHNICIAN) {

            task.setStatus(
                    RepairStatus.COMPLETED
            );

            task.setCompletedAt(
                    LocalDateTime.now()
            );

            repairTaskRepository.save(task);

            /*
             * Check whether all RepairTasks belonging to this
             * JobCard are now completed.
             */
            JobCard jobCard =
                    task.getJobCard();

            if (jobCard != null) {

                long total =
                        repairTaskRepository.countByJobCardId(
                                jobCard.getId()
                        );

                long completed =
                        repairTaskRepository.countByJobCardIdAndStatus(
                                jobCard.getId(),
                                RepairStatus.COMPLETED
                        );

                /*
                 * Only complete the JobCard when every RepairTask
                 * has been completed.
                 */
                if (total > 0 && total == completed) {

                    /*
                     * Preserve the existing JobCard transition
                     * validation from RepairTaskServiceImpl.
                     */
                    statusValidator.validate(
                            jobCard.getStatus(),
                            JobCardStatus.REPAIR_COMPLETED
                    );

                    jobCard.setStatus(
                            JobCardStatus.REPAIR_COMPLETED
                    );

                    jobCardRepository.save(
                            jobCard
                    );
                }
            }
        }

        assignment =
                jobAssignmentRepository.save(
                        assignment
                );

        return jobAssignmentMapper.toResponse(
                assignment
        );
    }

    private JobAssignment getAssignmentOrThrow(
            Long assignmentId) {

        return jobAssignmentRepository
                .findById(assignmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assignment not found : "
                                        + assignmentId));
    }

    /**
     * Reassignment preserves history rather than mutating the existing
     * row in place: the old JobAssignment is marked CANCELLED and a new
     * JobAssignment is created for the new technician, carrying over the
     * same garage/job card/estimate item. Any RepairTask currently
     * pointing at the old assignment is re-pointed at the new one, so
     * RepairTask.jobAssignment always reflects current ownership while
     * the cancelled row remains queryable as history.
     */
    @Override
    @Transactional
    public JobAssignmentResponse reassignJob(
            Long assignmentId,
            ReassignJobRequest request) {

        JobAssignment oldAssignment =
                getAssignmentOrThrow(
                        assignmentId
                );

        User user =
                userRepository.findById(
                        request.getEmployeeId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found : "
                                        + request.getEmployeeId()
                        ));

        /*
         * Preserve the RepairTask linked to the old assignment.
         * The old assignment will be cancelled, but the RepairTask
         * must be transferred to the new assignment.
         */
        RepairTask repairTask =
                oldAssignment.getRepairTask();

        oldAssignment.setStatus(
                JobAssignmentStatus.CANCELLED
        );

        jobAssignmentRepository.save(
                oldAssignment
        );

        JobAssignment newAssignment =
                new JobAssignment();

        newAssignment.setGarage(
                oldAssignment.getGarage()
        );

        newAssignment.setJobCard(
                oldAssignment.getJobCard()
        );

        newAssignment.setEstimateItem(
                oldAssignment.getEstimateItem()
        );

        newAssignment.setRepairTask(
                repairTask
        );

        newAssignment.setUser(
                user
        );

        newAssignment.setAssignmentType(
                oldAssignment.getAssignmentType()
        );

        newAssignment.setAssignedAt(
                LocalDateTime.now()
        );

        newAssignment.setEstimatedHours(
                oldAssignment.getEstimatedHours()
        );

        newAssignment.setRemarks(
                request.getRemarks()
        );

        newAssignment.setStatus(
                JobAssignmentStatus.ASSIGNED
        );

        newAssignment =
                jobAssignmentRepository.save(
                        newAssignment
                );

        /*
         * RepairTask.jobAssignment now points to the NEW active
         * assignment while the old assignment remains CANCELLED
         * for history/audit.
         */
        linkRepairTaskToAssignment(
                newAssignment
        );

        return jobAssignmentMapper.toResponse(
                newAssignment
        );
    }

    @Override
    @Transactional(readOnly = true)
    public JobAssignmentResponse getAssignment(
            Long assignmentId) {

        JobAssignment assignment =
                getAssignmentOrThrow(
                        assignmentId
                );

        return jobAssignmentMapper.toResponse(
                assignment
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobAssignmentResponse> getAssignmentsByJobCard(
            Long jobCardId) {

        List<JobAssignment> assignments =
                jobAssignmentRepository.findByJobCardId(
                        jobCardId
                );

        return jobAssignmentMapper.toResponse(
                assignments
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyAssignmentResponse> getMyAssignments(
            Long userId) {

        List<JobAssignment> assignments =
                jobAssignmentRepository.findByUserId(
                        userId
                );

        /*
         * job_assignments is reassignment history, not a list of
         * currently-active work. A CANCELLED row must not show up
         * in "My Work".
         */
        List<JobAssignment> active =
                assignments.stream()
                        .filter(a ->
                                a.getStatus()
                                        != JobAssignmentStatus.CANCELLED)
                        .collect(Collectors.toList());

        return jobAssignmentMapper.toMyAssignment(
                active
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyAssignmentResponse> getMyDriverAssignments(
            Long userId) {

        List<JobAssignment> assignments =
                jobAssignmentRepository
                        .findByUserIdAndAssignmentType(
                                userId,
                                JobAssignmentType.DRIVER
                        );

        return jobAssignmentMapper.toMyAssignment(
                assignments
        );
    }

    private JobAssignment getAssignmentForUserOrThrow(
            Long assignmentId,
            Long userId) {

        JobAssignment assignment =
                jobAssignmentRepository.findById(
                        assignmentId
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assignment not found : "
                                        + assignmentId
                        )
                );

        if (!assignment.getUser().getId().equals(userId)) {

            throw new IllegalStateException(
                    "You are not authorized to access this assignment."
            );
        }

        return assignment;
    }
}