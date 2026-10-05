package com.garageos.modules.customer.repository;

import com.garageos.core.enums.customer.CustomerGarageSource;
import com.garageos.modules.customer.entity.CustomerGarage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CustomerGarageRepository extends JpaRepository<CustomerGarage, Long> {

    List<CustomerGarage> findByCustomerId(Long customerId);

    Optional<CustomerGarage> findByCustomerIdAndGarageId(Long customerId, Long garageId);

    List<CustomerGarage> findByCustomerIdOrderByCreatedAtAsc(Long customerId);

    boolean existsByCustomerIdAndGarageId(Long customerId, Long garageId);

    boolean existsByCustomerId(Long customerId);

    boolean existsByCustomerIdAndDefaultGarageTrue(Long customerId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update CustomerGarage cg set cg.defaultGarage = false "
            + "where cg.customerId = :customerId and cg.defaultGarage = true")
    int clearDefault(@Param("customerId") Long customerId);

    boolean existsByCustomerIdAndGarageIdIn(Long customerId, Collection<Long> garageIds);
}
