package com.garageos.modules.navigation.repository;

import com.garageos.modules.navigation.entity.DriverCurrentLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverCurrentLocationRepository
        extends JpaRepository<DriverCurrentLocation, Long> {

    Optional<DriverCurrentLocation> findByDriverId(Long driverId);
}