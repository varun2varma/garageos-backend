package com.garageos.modules.inspectionfinding.repository;

import com.garageos.core.enums.InspectionFindingStatus;
import com.garageos.modules.inspectionfinding.entity.InspectionFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InspectionFindingRepository
        extends JpaRepository<InspectionFinding, Long> {

    List<InspectionFinding> findByJobCardIdOrderById(Long jobCardId);

    boolean existsByJobCardId(Long jobCardId);

    List<InspectionFinding> findByJobCardIdAndStatus(
            Long jobCardId,
            InspectionFindingStatus status);

    long countByJobCardId(Long jobCardId);

    long countByJobCardIdAndStatus(
            Long jobCardId,
            InspectionFindingStatus status);

    List<InspectionFinding> findByJobCardIdAndStatusIn(
            Long jobCardId,
            List<InspectionFindingStatus> statuses);
}