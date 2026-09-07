package com.garageos.modules.navigation.repository;

import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.core.enums.navigation.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NavigationTripRepository
        extends JpaRepository<NavigationTrip, Long> {

    List<NavigationTrip>
    findByDriverIdAndStatusIn(
            Long driverId,
            List<TripStatus> statuses
    );

    Optional<NavigationTrip>
    findByIdAndDriverId(
            Long tripId,
            Long driverId
    );

    List<NavigationTrip>
    findByVehicleIdOrderByCreatedAtDesc(
            Long vehicleId
    );

    Optional<NavigationTrip>
    findFirstByVehicleIdAndStatusInOrderByCreatedAtDesc(
            Long vehicleId,
            List<TripStatus> statuses
    );
}