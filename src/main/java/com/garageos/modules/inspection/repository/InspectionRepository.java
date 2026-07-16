package com.garageos.modules.inspection.repository;

import com.garageos.modules.inspection.entity.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InspectionRepository
        extends JpaRepository<Inspection, Long> {

    List<Inspection> findByComplaintId(Long complaintId);

}