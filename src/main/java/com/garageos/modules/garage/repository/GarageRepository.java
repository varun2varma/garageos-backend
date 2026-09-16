package com.garageos.modules.garage.repository;

import com.garageos.modules.garage.entity.Garage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GarageRepository
        extends JpaRepository<Garage, Long> {

    Optional<Garage> findByGarageCode(String garageCode);

    boolean existsByGarageCode(String garageCode);

    /**
     * Pessimistically locks the Garage row so
     * next_employee_sequence can be read-and-incremented without a
     * race between concurrent employee onboarding approvals for the
     * same garage. Only call within an existing transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Garage g where g.id = :id")
    Optional<Garage> findByIdForUpdate(@Param("id") Long id);

}