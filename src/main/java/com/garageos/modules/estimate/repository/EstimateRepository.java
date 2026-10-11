package com.garageos.modules.estimate.repository;

import com.garageos.core.enums.EstimateStatus;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.estimate.entity.Estimate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EstimateRepository extends JpaRepository<Estimate, Long> {

    /**
     * Row lock (SELECT ... FOR UPDATE) used to serialise invoice issuance and approved-line classification
     * correction on the same estimate. Both paths take it before checking invoice existence.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Estimate e where e.id = :id")
    Optional<Estimate> findByIdForUpdate(@Param("id") Long id);

    /** Same row lock, addressed by job card (employee approval entry point). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Estimate e where e.jobCard.id = :jobCardId")
    Optional<Estimate> findByJobCardIdForUpdate(@Param("jobCardId") Long jobCardId);

    /**
     * The status as stored right now. A scalar query, so it is never answered from an entity already cached in the
     * persistence context: used to re-validate state AFTER the row lock has been taken.
     */
    @Query("select e.status from Estimate e where e.id = :id")
    Optional<EstimateStatus> findStatusById(@Param("id") Long id);

    Optional<Estimate> findByEstimateNumber(String estimateNumber);

    Optional<Estimate> findTopByOrderByIdDesc();

    boolean existsByJobCardId(Long jobCardId);

    Optional<Estimate> findByJobCardId(Long jobCardId);

    long countByStatus(EstimateStatus status);

    List<Estimate> findByJobCardCustomer(Customer customer);

    long countByJobCardCustomer(Customer customer);

    Optional<Estimate> findTopByJobCardGarageIdOrderByIdDesc(
            Long garageId);


    List<Estimate> findByJobCardGarageIdIn(java.util.Collection<Long> garageIds);

    List<Estimate> findByJobCardCustomerMobileNumber(String mobileNumber);
}
