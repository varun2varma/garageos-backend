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
}