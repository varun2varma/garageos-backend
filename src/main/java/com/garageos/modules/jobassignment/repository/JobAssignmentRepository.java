package com.garageos.modules.jobassignment.repository;

import com.garageos.core.enums.JobAssignmentStatus;
import com.garageos.core.enums.JobAssignmentType;
import com.garageos.modules.jobassignment.entity.JobAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobAssignmentRepository
        extends JpaRepository<JobAssignment, Long> {

    List<JobAssignment> findByUserId(Long userId);

    List<JobAssignment> findByJobCardId(Long jobCardId);

    List<JobAssignment> findByStatus(JobAssignmentStatus status);

    List<JobAssignment> findByUserIdAndStatus(
            Long userId,
            JobAssignmentStatus status
    );

    List<JobAssignment> findByUserIdAndAssignmentType(
            Long userId,
            JobAssignmentType assignmentType
    );
}