package com.garageos.modules.catalog.repository;

import com.garageos.modules.catalog.entity.GstTaxRule;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface GstTaxRuleRepository extends JpaRepository<GstTaxRule, Long> {

    boolean existsByRuleCode(String ruleCode);

    List<GstTaxRule> findByRuleCodeOrderByEffectiveFromDesc(String ruleCode);

    List<GstTaxRule> findAllByOrderByRuleCodeAscEffectiveFromDesc();

    /** Rules in force on {@code on}, newest first; callers use the first element. */
    @Query("""
            SELECT r FROM GstTaxRule r
            WHERE r.ruleCode = :code AND r.active = true
              AND r.effectiveFrom <= :on
              AND (r.effectiveTo IS NULL OR r.effectiveTo >= :on)
            ORDER BY r.effectiveFrom DESC
            """)
    List<GstTaxRule> findEffective(
            @Param("code") String code,
            @Param("on") LocalDate on,
            Pageable pageable);
}
