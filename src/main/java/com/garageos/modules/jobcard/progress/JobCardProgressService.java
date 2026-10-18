package com.garageos.modules.jobcard.progress;

import com.garageos.core.enums.RepairStatus;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.repairtask.entity.RepairTask;
import com.garageos.modules.repairtask.repository.RepairTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Loads the persisted task facts and delegates to {@link JobCardProgressCalculator}. */
@Service
@RequiredArgsConstructor
public class JobCardProgressService {

    private final RepairTaskRepository repairTaskRepository;

    @Transactional(readOnly = true)
    public JobCardProgressCalculator.Result progressOf(JobCard jobCard) {

        List<JobCardProgressCalculator.TaskState> tasks = repairTaskRepository
                .findByJobCardIdOrderById(jobCard.getId()).stream()
                .map(JobCardProgressService::toState)
                .toList();

        return JobCardProgressCalculator.compute(jobCard.getStatus(), tasks);
    }

    static JobCardProgressCalculator.TaskState toState(RepairTask t) {

        boolean cancelled = t.getStatus() == RepairStatus.CANCELLED;
        boolean completed = t.getStatus() == RepairStatus.COMPLETED || t.getCompletedAt() != null;
        boolean started = t.getStartedAt() != null || t.getStatus() == RepairStatus.IN_PROGRESS;
        boolean accepted = t.getJobAssignment() != null && t.getJobAssignment().getAcceptedAt() != null;
        boolean assigned = t.getAssignedAt() != null || t.getJobAssignment() != null
                || t.getStatus() == RepairStatus.ASSIGNED;

        return new JobCardProgressCalculator.TaskState(cancelled, assigned, accepted, started, completed);
    }
}
