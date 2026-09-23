package com.garageos.modules.navigation.repository;

import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.core.enums.navigation.TripStatus;
import org.springframework.data.domain.Pageable;
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

    /**
     * Driver trip history - newest first, capped by the caller's
     * [Pageable] rather than returning every trip a driver has ever run.
     */
    List<NavigationTrip>
    findByDriverIdAndStatusInOrderByIdDesc(
            Long driverId,
            List<TripStatus> statuses,
            Pageable pageable
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

    Optional<NavigationTrip>
    findFirstByNavigationRequestIdOrderByIdDesc(
            Long navigationRequestId
    );

    /**
     * Batched across a customer's whole vehicle list (Customer Live
     * Vehicle Journey, see CustomerVehicleJourneyServiceImpl) - one query
     * for every vehicle rather than one query per vehicle. TripStatus
     * (not NavigationRequestStatus) is the authority on whether a trip is
     * still actually active - see that class for why.
     */
    List<NavigationTrip>
    findByVehicleIdInAndStatusIn(
            List<Long> vehicleIds,
            List<TripStatus> statuses
    );
}