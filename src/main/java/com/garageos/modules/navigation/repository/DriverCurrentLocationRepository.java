package com.garageos.modules.navigation.repository;

import com.garageos.modules.navigation.entity.DriverCurrentLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverCurrentLocationRepository
        extends JpaRepository<DriverCurrentLocation, Long> {

    Optional<DriverCurrentLocation> findByDriverId(Long driverId);

    Optional<DriverCurrentLocation>
    findByTripId(Long tripId);

    /** Each listed driver's most recent position (one query for the whole fleet; ties resolved by the caller). */
    @Query("select l from DriverCurrentLocation l where l.driverId in :driverIds and l.lastUpdated = "
            + "(select max(l2.lastUpdated) from DriverCurrentLocation l2 where l2.driverId = l.driverId)")
    List<DriverCurrentLocation> findLatestForDrivers(@Param("driverIds") Collection<Long> driverIds);
}
