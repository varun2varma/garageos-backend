package com.garageos.modules.jobcard.repository;

import com.garageos.modules.jobcard.entity.JobCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobCardRepository extends JpaRepository<JobCard, Long> {

    boolean existsByJobCardNumber(String jobCardNumber);

    Optional<JobCard> findByJobCardNumber(String jobCardNumber);

    Optional<JobCard> findTopByOrderByIdDesc();

}