package com.garageos.modules.jobcard.repository;

import com.garageos.core.enums.JobCardStatus;
import com.garageos.modules.jobcard.entity.JobCard;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JobCardRepository extends JpaRepository<JobCard, Long> {

    boolean existsByJobCardNumber(String jobCardNumber);

    Optional<JobCard> findByJobCardNumber(String jobCardNumber);

    Optional<JobCard> findTopByOrderByIdDesc();

    long countByStatusNotIn(List<JobCardStatus> statuses);

    long countByStatus(JobCardStatus status);

    long countByServiceDate(LocalDate serviceDate);

    List<JobCard> findAllByOrderByCreatedAtDesc(Pageable pageable);

}