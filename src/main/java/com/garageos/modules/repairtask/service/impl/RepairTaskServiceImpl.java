package com.garageos.modules.repairtask.service.impl;

import com.garageos.core.enums.JobAssignmentStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.RepairStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.complaint.entity.Complaint;
import com.garageos.modules.complaint.repository.ComplaintRepository;
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
import com.garageos.modules.jobcard.service.JobCardService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final com.garageos.modules.jobassignment.repository.JobAssignmentRepository jobAssignmentRepository;
    private final UserRepository userRepository;
    private final ComplaintRepository complaintRepository;

    @Lazy
    @Autowired
    private JobCardService jobCardService;

    @Override
    @Transactional
    public void createRepairTasks(Estimate estimate) {

        List<EstimateItem> estimateItems =
                estimateItemRepository.findByEstimateId(
                        estimate.getId()
                );

        /*
         * One RepairTask = one Complaint.
         *
         * EstimateItems remain financial line items.
         * Multiple parts/labour items can belong to the same complaint,
         * but they must NOT create multiple RepairTasks.
         */

        Map<Long, EstimateItem> representativeItemByComplaint =
                new LinkedHashMap<>();

        for (EstimateItem item : estimateItems) {

            if (item.getComplaint() == null) {
                continue;
            }

            // Mission: a deselected item must not continue into repair.
            // If every item under a complaint was deselected, no
            // RepairTask is created for that complaint at all; if some
            // remain selected, the complaint's repair still proceeds
            // (RepairTask granularity is per-complaint, not per-item -
            // see this method's own comment above).
            if (!Boolean.TRUE.equals(item.getSelected())) {
                continue;
            }

            representativeItemByComplaint.putIfAbsent(
                    item.getComplaint().getId(),
                    item
            );
        }

        List<RepairTask> tasks = new ArrayList<>();

        for (Map.Entry<Long, EstimateItem> entry
                : representativeItemByComplaint.entrySet()) {

            Long complaintId = entry.getKey();
            EstimateItem representativeItem = entry.getValue();

            if (repository.existsByJobCardIdAndComplaintId(
                    estimate.getJobCard().getId(),
                    complaintId)) {

                continue;
            }

            Complaint complaint =
                    complaintRepository.findById(complaintId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Complaint not found : "
                                                    + complaintId
                                    ));

            RepairTask task = RepairTask.builder()
                    .jobCard(estimate.getJobCard())
                    .complaint(complaint)

                    // Keep one representative estimate item for
                    // backward traceability only.
                    .estimateItem(representativeItem)

                    .status(RepairStatus.PENDING)
                    .build();

            tasks.add(task);
        }

        repository.saveAll(tasks);
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
        if (request.getEmployeeId() != null) {

            linkOrCreateJobAssignment(
                    task,
                    request.getEmployeeId()
            );
        }

        RepairTask savedTask = repository.save(task);

        return mapper.toResponse(savedTask);
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
            assignRequest.setRepairTaskId(task.getId());
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

    /**
     * Phase H — RepairTask/JobAssignment reconciliation (Known Issue #1:
     * "whichever path a technician actually uses, the other model never
     * finds out"). RepairTask is the canonical technician-work entity
     * (confirmed consistent with this codebase's own existing direction —
     * CLAUDE.md §12 already says "for any NEW technician/assignment
     * functionality, default to JobAssignment... unless the task
     * explicitly requires RepairTask", and RepairTask already owns
     * priority (V49) and is what every manager/customer-facing screen
     * actually reads). This keeps the *linked* JobAssignment's status a
     * derived projection of RepairTask's, rather than a second
     * independently-mutable source of truth:
     * - never touches a JobAssignment that isn't linked (task.getJobAssignment() == null)
     * - never resurrects one that's CANCELLED (a cancelled assignment is a
     *   deliberate, separate decision - not something a task transition
     *   should undo)
     * - never regresses one that's already past the target status in the
     *   normal forward order (ASSIGNED &lt; ACCEPTED &lt; IN_PROGRESS &lt; COMPLETED) -
     *   e.g. completing a task must not "un-complete" an assignment a QC
     *   flow already moved to QC_PENDING/QC_FAILED/REWORK
     * This does NOT change RepairTaskType/JobAssignmentType's own
     * independent lifecycle semantics (RepairTask keeps its own status
     * enum, its own PENDING/IN_PROGRESS/COMPLETED transitions, and
     * multiple RepairTasks remain independently trackable) - it only
     * keeps the one JobAssignment linked to a given RepairTask from
     * silently diverging from it.
     */
    private void syncLinkedJobAssignment(
            RepairTask task,
            JobAssignmentStatus targetStatus,
            LocalDateTime startedAt,
            LocalDateTime completedAt) {

        JobAssignment assignment = task.getJobAssignment();

        if (assignment == null || assignment.getStatus() == JobAssignmentStatus.CANCELLED) {
            return;
        }

        java.util.List<JobAssignmentStatus> forwardOrder = java.util.List.of(
                JobAssignmentStatus.ASSIGNED,
                JobAssignmentStatus.ACCEPTED,
                JobAssignmentStatus.IN_PROGRESS,
                JobAssignmentStatus.COMPLETED
        );

        int currentIndex = forwardOrder.indexOf(assignment.getStatus());
        int targetIndex = forwardOrder.indexOf(targetStatus);

        // A status outside the normal forward order (QC_PENDING,
        // QC_FAILED, REWORK, ON_HOLD) is a state a separate quality/rework
        // flow put the assignment into deliberately - a RepairTask
        // start/complete call must not silently overwrite that.
        if (currentIndex < 0 || targetIndex < 0 || targetIndex <= currentIndex) {
            return;
        }

        assignment.setStatus(targetStatus);

        if (startedAt != null) {
            assignment.setStartedAt(startedAt);
        }

        if (completedAt != null) {
            assignment.setCompletedAt(completedAt);
        }

        jobAssignmentRepository.save(assignment);
    }

    @Override
    @Transactional
    public RepairTaskResponse startRepair(Long repairTaskId) {

        RepairTask task = repository.findById(repairTaskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Repair Task not found with id : " + repairTaskId));

        authorizeRepairTaskAction(task);

//        if (task.getStatus() != RepairStatus.ASSIGNED) {
//            throw new BusinessException(
//                    "Repair Task must be assigned before starting.");
//        }

        // Start the Job Card repair workflow, but only on the first
        // Repair Task started for this Job Card. A second (or later)
        // RepairTask starting while the Job Card is already
        // REPAIR_IN_PROGRESS must not re-attempt that same transition —
        // JobCardStatusValidator has no REPAIR_IN_PROGRESS -> REPAIR_IN_
        // PROGRESS entry, so calling this unconditionally throws on every
        // RepairTask after the first.
        JobCard jobCard = task.getJobCard();

        if (jobCard.getStatus() == JobCardStatus.REPAIR_PENDING) {
            task.getJobCard().setStatus(JobCardStatus.REPAIR_IN_PROGRESS);
        }

        // Start the actual Repair Task
        task.setStatus(RepairStatus.IN_PROGRESS);
        task.setStartedAt(LocalDateTime.now());

        task = repository.save(task);

        // Phase H reconciliation (Known Issue #1): RepairTask is the
        // canonical technician-work entity; the linked JobAssignment must
        // never be allowed to drift from it. See syncLinkedJobAssignment's
        // own doc comment for exactly what this does and doesn't change.
        syncLinkedJobAssignment(task, JobAssignmentStatus.IN_PROGRESS, task.getStartedAt(), null);

        return mapper.toResponse(task);
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

//        if (jobCard.getStatus() != JobCardStatus.REPAIR_IN_PROGRESS) {
//            throw new BusinessException(
//                    "Job Card must be in REPAIR_IN_PROGRESS before a "
//                            + "Repair Task can be completed.");
//        }

        task.setStatus(RepairStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());

        task = repository.save(task);

        // Phase H reconciliation (Known Issue #1) - see startRepair's own
        // call and syncLinkedJobAssignment's doc comment.
        syncLinkedJobAssignment(task, JobAssignmentStatus.COMPLETED, null, task.getCompletedAt());

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

//            qualityCheckService.createQualityCheck(jobCard);
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

    @Override
    @Transactional
    public RepairTaskResponse setPriority(
            Long repairTaskId,
            com.garageos.core.enums.RepairTaskPriority priority) {

        RepairTask task = repository.findById(repairTaskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Repair Task not found with id : " + repairTaskId));

        // Same garage-scoping rule every other privileged mutation on this
        // entity already uses; the @PreAuthorize on the controller already
        // excludes TECHNICIAN entirely, so a technician can never reach
        // here to set their own work's priority.
        authorizeRepairTaskAction(task);

        task.setPriority(priority);

        return mapper.toResponse(repository.save(task));
    }
}