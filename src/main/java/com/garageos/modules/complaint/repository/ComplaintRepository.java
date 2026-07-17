package com.garageos.modules.complaint.repository;

import com.garageos.modules.complaint.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByJobCardId(Long jobCardId);
    Optional<Complaint> findFirstByJobCardId(Long jobCardId);
}