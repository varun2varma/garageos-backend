package com.garageos.modules.handover.repository;

import com.garageos.core.enums.navigation.HandoverStatus;
import com.garageos.core.enums.navigation.TripType;
import com.garageos.modules.handover.entity.VehicleHandover;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleHandoverRepository extends JpaRepository<VehicleHandover, Long> {

    Optional<VehicleHandover> findFirstByTripIdAndStatusOrderByCreatedAtDesc(
            Long tripId,
            HandoverStatus status
    );

    Optional<VehicleHandover> findFirstByTripIdOrderByCreatedAtDesc(Long tripId);

    /**
     * The trip-gating check: is there a VERIFIED handover for exactly this
     * trip and direction? Direction is included explicitly so a PICKUP
     * handover can never be mistaken for authorizing a DELIVERY completion
     * (or vice versa) even though today a trip's own tripType already
     * matches one direction only - this keeps the guarantee correct even
     * if that assumption ever changes.
     */
    boolean existsByTripIdAndDirectionAndStatus(
            Long tripId,
            TripType direction,
            HandoverStatus status
    );
}
