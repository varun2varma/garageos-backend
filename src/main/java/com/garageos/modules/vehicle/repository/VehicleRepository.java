package com.garageos.modules.vehicle.repository;

import com.garageos.modules.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByRegistrationNumber(String registrationNumber);

    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);
}