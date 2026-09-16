package com.garageos.modules.repairtask.repository;

import com.garageos.core.enums.RepairStatus;
import com.garageos.modules.repairtask.entity.RepairTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepairTaskRepository
        extends JpaRepository<RepairTask, Long> {

    List<RepairTask> findByJobCardIdOrderById(Long jobCardId);

    List<RepairTask> findByJobCardJobCardNumberOrderById(
            String jobCardNumber);

    List<RepairTask> findByStatus(RepairStatus status);

    long countByJobCardId(Long jobCardId);

    long countByJobCardIdAndStatus(
            Long jobCardId,
            RepairStatus status);

    boolean existsByEstimateItemId(Long estimateItemId);

    Optional<RepairTask> findByEstimateItemId(Long estimateItemId);

    Optional<RepairTask> findByJobAssignmentId(Long jobAssignmentId);

}