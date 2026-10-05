package com.garageos.modules.vehicle.repository;

import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByRegistrationNumber(String registrationNumber);

    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);

    List<Vehicle> findByCustomer(Customer customer);

    long countByCustomer(Customer customer);

    Page<Vehicle> findByCustomerId(Long customerId, Pageable pageable);

    /** Vehicles whose owner is related to (visible to) at least one of the given garages. */
    @Query("""
            SELECT v FROM Vehicle v
            WHERE EXISTS (SELECT 1 FROM CustomerGarage cg
                          WHERE cg.customerId = v.customer.id AND cg.garageId IN :garageIds)
            """)
    Page<Vehicle> findVisibleToGarages(
            @Param("garageIds") Collection<Long> garageIds, Pageable pageable);
}
