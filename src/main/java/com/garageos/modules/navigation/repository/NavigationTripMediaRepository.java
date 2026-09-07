package com.garageos.modules.navigation.repository;

import com.garageos.modules.navigation.entity.NavigationTripMedia;
import com.garageos.core.enums.navigation.TripMediaStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NavigationTripMediaRepository
        extends JpaRepository<NavigationTripMedia, Long> {

    List<NavigationTripMedia>
    findByTripIdOrderByCapturedAtAsc(
            Long tripId
    );

    List<NavigationTripMedia>
    findByTripIdAndMediaStageOrderByCapturedAtAsc(
            Long tripId,
            TripMediaStage mediaStage
    );
}