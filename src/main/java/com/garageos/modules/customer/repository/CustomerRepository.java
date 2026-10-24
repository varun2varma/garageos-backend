package com.garageos.modules.customer.repository;

import com.garageos.modules.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Pessimistically locks the customer row. Serialises work that must be one-at-a-time per customer, such as
     * choosing the single default garage. Only call within an existing transaction.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.id = :id")
    Optional<Customer> findByIdForUpdate(@Param("id") Long id);


    Optional<Customer> findByMobileNumber(String mobileNumber);

    boolean existsByMobileNumber(String mobileNumber);


    /** Customers related to (visible to) at least one of the given garages. */
    @Query("""
            SELECT c FROM Customer c
            WHERE EXISTS (SELECT 1 FROM CustomerGarage cg
                          WHERE cg.customerId = c.id AND cg.garageId IN :garageIds)
            """)
    Page<Customer> findVisibleToGarages(
            @Param("garageIds") Collection<Long> garageIds, Pageable pageable);

    @Query("""
            SELECT c FROM Customer c
            WHERE c.mobileNumber = :mobileNumber
              AND EXISTS (SELECT 1 FROM CustomerGarage cg
                          WHERE cg.customerId = c.id AND cg.garageId IN :garageIds)
            """)
    Optional<Customer> findVisibleByMobileNumber(
            @Param("mobileNumber") String mobileNumber,
            @Param("garageIds") Collection<Long> garageIds);

}
