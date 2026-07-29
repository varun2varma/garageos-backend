package com.garageos.modules.garage.repository;

import com.garageos.modules.garage.entity.Garage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GarageRepository
        extends JpaRepository<Garage, Long> {

    Optional<Garage> findByGarageCode(String garageCode);

    boolean existsByGarageCode(String garageCode);

}