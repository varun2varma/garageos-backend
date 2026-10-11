package com.garageos.modules.garage.repository;

import com.garageos.modules.garage.entity.Garage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface GarageRepository
        extends JpaRepository<Garage, Long> {

    Optional<Garage> findByGarageCode(String garageCode);

    boolean existsByGarageCode(String garageCode);

    /**
     * Guards against registering the same garage twice for one owner (a double-tapped or retried submission).
     * Same name, address and city, ignoring case, under the same owner.
     */
    boolean existsByOwnerUserIdAndGarageNameIgnoreCaseAndAddressIgnoreCaseAndCityIgnoreCase(
            Long ownerUserId, String garageName, String address, String city);

    /**
     * Pessimistically locks the Garage row so
     * next_employee_sequence can be read-and-incremented without a
     * race between concurrent employee onboarding approvals for the
     * same garage. Only call within an existing transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Garage g where g.id = :id")
    Optional<Garage> findByIdForUpdate(@Param("id") Long id);


    List<Garage> findByStatus(com.garageos.core.enums.garage.GarageStatus status);

    /** Bounding-box prefilter for nearby discovery: ACTIVE garages with usable coordinates only. */
    @Query("""
            SELECT g FROM Garage g
            WHERE g.status = com.garageos.core.enums.garage.GarageStatus.ACTIVE
              AND g.latitude IS NOT NULL AND g.longitude IS NOT NULL
              AND g.latitude BETWEEN :minLat AND :maxLat
              AND g.longitude BETWEEN :minLon AND :maxLon
            """)
    List<Garage> findActiveWithinBox(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLon") BigDecimal minLon,
            @Param("maxLon") BigDecimal maxLon);
}
