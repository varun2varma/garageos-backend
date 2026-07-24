package com.garageos.modules.repairtask.service.impl;

import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.RepairStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.estimateitem.repository.EstimateItemRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.qualitycheck.service.QualityCheckService;
import com.garageos.modules.repairtask.dto.response.RepairTaskResponse;
import com.garageos.modules.repairtask.entity.RepairTask;
import com.garageos.modules.repairtask.mapper.RepairTaskMapper;
import com.garageos.modules.repairtask.repository.RepairTaskRepository;
import com.garageos.modules.repairtask.service.RepairTaskService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

    @Override
    public RepairTaskResponse assignTechnician(
            Long repairTaskId,
            String technicianName) {

        RepairTask task = repository.findById(repairTaskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Repair Task not found with id : "
                                        + repairTaskId));

        task.setTechnicianName(technicianName);
        task.setAssignedAt(LocalDateTime.now());
        task.setStatus(RepairStatus.ASSIGNED);

        return mapper.toResponse(repository.save(task));
    }

    @Override
    public RepairTaskResponse startRepair(Long repairTaskId) {

        RepairTask task = repository.findById(repairTaskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Repair Task not found with id : "
                                        + repairTaskId));

        if (task.getStatus() != RepairStatus.ASSIGNED) {
            throw new BusinessException(
                    "Repair Task must be assigned before starting.");
        }

        task.setStatus(RepairStatus.IN_PROGRESS);
        task.setStartedAt(LocalDateTime.now());

        return mapper.toResponse(repository.save(task));
    }

    @Override
    @Transactional
    public RepairTaskResponse completeRepair(Long repairTaskId) {

        RepairTask task = repository.findById(repairTaskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Repair Task not found with id : "
                                        + repairTaskId));

        task.setStatus(RepairStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());

        task = repository.save(task);

        JobCard jobCard = task.getJobCard();

        long total =
                repository.countByJobCardId(jobCard.getId());

        long completed =
                repository.countByJobCardIdAndStatus(
                        jobCard.getId(),
                        RepairStatus.COMPLETED);

        if (total == completed) {

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