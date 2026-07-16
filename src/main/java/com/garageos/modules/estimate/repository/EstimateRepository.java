package com.garageos.modules.estimate.repository;

import com.garageos.modules.estimate.entity.Estimate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstimateRepository extends JpaRepository<Estimate, Long> {

    Optional<Estimate> findByEstimateNumber(String estimateNumber);
    Optional<Estimate> findTopByOrderByIdDesc();
}