package com.garageos.modules.inspection.repository;

import com.garageos.modules.inspection.entity.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InspectionRepository
        extends JpaRepository<Inspection, Long> {

    Optional<Inspection> findByComplaintId(Long complaintId);
    Optional<Inspection> findFirstByComplaintId(Long complaintId);
    List<Inspection> findByComplaintJobCardId(Long jobCardId);
}