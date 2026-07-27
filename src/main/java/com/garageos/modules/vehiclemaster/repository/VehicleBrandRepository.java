package com.garageos.modules.vehiclemaster.repository;

import com.garageos.modules.vehiclemaster.entity.VehicleBrand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleBrandRepository extends JpaRepository<VehicleBrand, Long> {

    List<VehicleBrand> findAll();
    Optional<VehicleBrand> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<VehicleBrand> findAllByOrderByNameAsc();
}