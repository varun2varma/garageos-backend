package com.garageos.modules.repairtask.service.impl;

import com.garageos.core.enums.JobAssignmentStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.RepairStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.estimateitem.repository.EstimateItemRepository;
import com.garageos.core.enums.JobAssignmentType;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.jobassignment.dto.request.AssignJobRequest;
import com.garageos.modules.jobassignment.dto.request.ReassignJobRequest;
import com.garageos.modules.jobassignment.entity.JobAssignment;
import com.garageos.modules.jobassignment.service.JobAssignmentService;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.jobcard.validator.JobCardStatusValidator;
import com.garageos.modules.qualitycheck.service.QualityCheckService;
import com.garageos.modules.repairtask.dto.request.AssignTechnicianRequest;
import com.garageos.modules.repairtask.dto.response.RepairTaskResponse;
import com.garageos.modules.repairtask.entity.RepairTask;
import com.garageos.modules.repairtask.mapper.RepairTaskMapper;
import com.garageos.modules.repairtask.repository.RepairTaskRepository;
import com.garageos.modules.repairtask.service.RepairTaskService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RepairTaskServiceImpl implements RepairTaskService {

    private final RepairTaskRepository repository;
    private final EstimateItemRepository estimateItemRepository;
    private final JobCardRepository jobCardRepository;
    private final RepairTaskMapper mapper;
    private final QualityCheckService qualityCheckService;
    private final JobCardStatusValidator statusValidator;
    private final JobAssignmentService jobAssignmentService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void createRepairTasks(Estimate estimate) {

        List<EstimateItem> estimateItems =
                estimateItemRepository.findByEstimateId(estimate.getId());

        List<RepairTask> tasks = new ArrayList<>();

        for (EstimateItem item : estimateItems) {

            if (repository.existsByEstimateItemId(item.getId())) {
                continue;
            }

            RepairTask task = RepairTask.builder()
                    .jobCard(estimate.getJobCard())
                    .estimateItem(item)
                    .status(RepairStatus.PENDING)
                    .build();

            tasks.add(task);
        }

        repository.saveAll(tasks);

//        JobCard jobCard = estimate.getJobCard();
//
//        jobCard.setStatus(JobCardStatus.REPAIR_PENDING);
//
//        jobCardRepository.save(jobCard);
    }

    /**
     * Corrective fix for audit Finding T1. technicianName remains a
     * display snapshot only, exactly as before. When the request also
     * supplies employeeId (additive, optional — every currently-known
     * caller omits it, so their behavior is unchanged), this converges
     * the legacy free-text endpoint into the authoritative JobAssignment
     * model by reusing JobAssignmentService's own assign/reassign
     * operations (same garage-scope and reassignment-history rules as
     * the primary JobAssignment endpoints — no third assignment
     * mechanism is created here).
     */
    @Override
    @Transactional
    public RepairTaskResponse assignTechnician(
            Long repairTaskId,
            AssignTechnicianRequest request) {

        RepairTask task = repository.findById(repairTaskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Repair Task not found with id : "
                                        + repairTaskId));

        authorizeRepairTaskAction(task);

        String technicianName = request.getTechnicianName();

        if (technicianName == null || technicianName.isBlank()) {
            throw new BusinessException(
                    "Technician name is required.");
        }
        task.setTechnicianName(technicianName);
        task.setAssignedAt(LocalDateTime.now());
        task.setStatus(RepairStatus.ASSIGNED);

        task = repository.save(task);

        if (request.getEmployeeId() != null) {
            linkOrCreateJobAssignment(task, request.getEmployeeId());
        }

        return mapper.toResponse(task);
    }

    private void linkOrCreateJobAssignment(RepairTask task, Long employeeId) {

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found : " + employeeId));

        if (task.getJobCard().getGarage() == null
                || employee.getGarageId() == null
                || !employee.getGarageId()
                        .equals(task.getJobCard().getGarage().getId())) {

            throw new BusinessException(
                    "Technician does not belong to this Job Card's garage.");
        }

        JobAssignment existing = task.getJobAssignment();

        if (existing != null
                && existing.getStatus() != JobAssignmentStatus.CANCELLED) {

            ReassignJobRequest reassignRequest = new ReassignJobRequest();
            reassignRequest.setEmployeeId(employeeId);

            jobAssignmentService.reassignJob(existing.getId(), reassignRequest);

        } else {

            AssignJobRequest assignRequest = new AssignJobRequest();
            assignRequest.setJobCardId(task.getJobCard().getId());
            assignRequest.setEstimateItemId(task.getEstimateItem().getId());
            assignRequest.setEmployeeId(employeeId);
            assignRequest.setAssignmentType(JobAssignmentType.TECHNICIAN);

            jobAssignmentService.assignJob(assignRequest);
        }
    }

    /**
     * Technician access is scoped to jobs actually assigned to them:
     * same garage plus a non-cancelled JobAssignment on this specific
     * RepairTask (mirrors the existing, working Media authorization
     * pattern). Non-technician callers are restricted to the locked
     * operational JobCard roles (Manager/Owner/Service Advisor) —
     * corrective fix for audit Finding P2: previously any authenticated
     * employee role (Accountant, Cashier, Inventory Manager, Driver)
     * could start/complete a Repair Task with no restriction at all.
     */
    private void authorizeRepairTaskAction(RepairTask task) {

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        if (principal.getRoles().contains(RoleCode.TECHNICIAN.name())) {

            JobAssignment assignment = task.getJobAssignment();

            if (assignment == null
                    || assignment.getStatus() == JobAssignmentStatus.CANCELLED
                    || !assignment.getUser().getId().equals(principal.getId())
                    || !assignment.getGarage().getId().equals(principal.getGarageId())) {

                throw new BusinessException(
                        "You are not assigned to this Repair Task.");
            }

            return;
        }

        if (principal.getRoles().contains(RoleCode.MANAGER.name())
                || principal.getRoles().contains(RoleCode.OWNER.name())
                || principal.getRoles().contains(RoleCode.SERVICE_ADVISOR.name())) {

            if (task.getJobCard().getGarage() == null
                    || principal.getGarageId() == null
                    || !principal.getGarageId()
                            .equals(task.getJobCard().getGarage().getId())) {

                throw new BusinessException(
                        "This Repair Task does not belong to your garage.");
            }

            return;
        }

        throw new BusinessException(
                "You are not authorized to perform this action.");
    }

    @Override
    public RepairTaskResponse startRepair(Long repairTaskId) {

        RepairTask task = repository.findById(repairTaskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Repair Task not found with id : "
                                        + repairTaskId));

        authorizeRepairTaskAction(task);

        if (task.getStatus() != RepairStatus.ASSIGNED) {
            throw new BusinessException(
                    "Repair Task must be assigned before starting.");
        }

        task.setStatus(RepairStatus.IN_PROGRESS);
        task.setStartedAt(LocalDateTime.now());

        return mapper.toResponse(repository.save(task));
    }

    /**
     * Corrective fix for audit Finding L1: a RepairTask can only be
     * completed while its JobCard is in REPAIR_IN_PROGRESS — this closes
     * the previously-possible REPAIR_PENDING -> REPAIR_COMPLETED skip
     * (completing tasks before the JobCard-level "start repair" action
     * had ever been called). The aggregate transition to
     * REPAIR_COMPLETED is additionally validated against
     * JobCardStatusValidator before being applied, rather than being
     * written unconditionally.
     */
    @Override
    @Transactional
    public RepairTaskResponse completeRepair(Long repairTaskId) {

        RepairTask task = repository.findById(repairTaskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Repair Task not found with id : "
                                        + repairTaskId));

        authorizeRepairTaskAction(task);

        JobCard jobCard = task.getJobCard();

        if (jobCard.getStatus() != JobCardStatus.REPAIR_IN_PROGRESS) {
            throw new BusinessException(
                    "Job Card must be in REPAIR_IN_PROGRESS before a "
                            + "Repair Task can be completed.");
        }

        task.setStatus(RepairStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());

        task = repository.save(task);

        long total =
                repository.countByJobCardId(jobCard.getId());

        long completed =
                repository.countByJobCardIdAndStatus(
                        jobCard.getId(),
                        RepairStatus.COMPLETED);

        if (total == completed) {

            statusValidator.validate(
                    jobCard.getStatus(),
                    JobCardStatus.REPAIR_COMPLETED
            );

            jobCard.setStatus(JobCardStatus.REPAIR_COMPLETED);

            jobCardRepository.save(jobCard);

            qualityCheckService.createQualityCheck(jobCard);
        }

        return mapper.toResponse(task);
    }

    @Override
    public List<RepairTaskResponse> getRepairTasks(Long jobCardId) {

        return mapper.toResponseList(
                repository.findByJobCardIdOrderById(jobCardId));
    }

    @Override
    public RepairTaskResponse getRepairTask(Long id) {

        RepairTask task = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Repair Task not found with id : " + id));

        return mapper.toResponse(task);
    }
}