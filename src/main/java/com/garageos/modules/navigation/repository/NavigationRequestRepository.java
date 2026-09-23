package com.garageos.modules.navigation.repository;

import com.garageos.core.enums.navigation.NavigationRequestStatus;
import com.garageos.modules.navigation.entity.NavigationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NavigationRequestRepository
        extends JpaRepository<NavigationRequest, Long> {

    List<NavigationRequest>
    findByGarageIdAndStatusOrderByScheduledAtAsc(
            Long garageId,
            NavigationRequestStatus status
    );

    List<NavigationRequest>
    findByCustomerIdOrderByCreatedAtDesc(
            Long customerId
    );

    List<NavigationRequest>
    findByVehicleIdOrderByCreatedAtDesc(
            Long vehicleId
    );

    /**
     * Batched across a customer's whole vehicle list (Customer Live
     * Vehicle Journey, see CustomerVehicleJourneyServiceImpl) - used only
     * for the REQUESTED status, i.e. "pickup confirmed but no driver
     * assigned yet". ASSIGNED/IN_PROGRESS/COMPLETED are deliberately not
     * queried here: once a driver is assigned, a NavigationTrip exists
     * and that trip's own TripStatus (not this request's status, which
     * never advances past ASSIGNED again - see NavigationTripServiceImpl)
     * is the authoritative signal for anything after this point.
     */
    List<NavigationRequest>
    findByVehicleIdInAndStatusIn(
            List<Long> vehicleIds,
            List<NavigationRequestStatus> statuses
    );
}