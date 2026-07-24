package com.garageos.modules.qualitycheck.repository;

import com.garageos.modules.qualitycheck.entity.QualityCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QualityCheckRepository
        extends JpaRepository<QualityCheck, Long> {

    Optional<QualityCheck> findByJobCardId(Long jobCardId);

    Optional<QualityCheck> findByJobCardJobCardNumber(
            String jobCardNumber);

    boolean existsByJobCardId(Long jobCardId);

}