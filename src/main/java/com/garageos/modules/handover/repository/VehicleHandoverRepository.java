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

    /** Row-locked read used by verify, so two concurrent verifications cannot both consume one code. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query(
            "select h from VehicleHandover h where h.tripId = :tripId and h.status = :status order by h.createdAt desc, h.id desc")
    java.util.List<VehicleHandover> lockPendingForTrip(
            @org.springframework.data.repository.query.Param("tripId") Long tripId,
            @org.springframework.data.repository.query.Param("status") HandoverStatus status);

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
