package com.garageos.modules.vehiclemaster.repository;

import com.garageos.modules.vehiclemaster.entity.VehicleBrand;
import com.garageos.modules.vehiclemaster.entity.VehicleModel;
import com.garageos.modules.vehiclemaster.enums.BodyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {

    List<VehicleModel> findAll();
    List<VehicleModel> findByBrandOrderByNameAsc(VehicleBrand brand);

    Optional<VehicleModel> findByBrandAndNameIgnoreCase(
            VehicleBrand brand,
            String name
    );

    boolean existsByBrandAndNameIgnoreCase(
            VehicleBrand brand,
            String name
    );

    List<VehicleModel> findByBrandIdOrderByNameAsc(Long brandId);

    @Query("""
    SELECT m
    FROM VehicleModel m
    JOIN FETCH m.brand
    """)
    List<VehicleModel> findAllWithBrand();

    @Query("""
        SELECT DISTINCT m.bodyType
        FROM VehicleModel m
        ORDER BY m.bodyType
        """)
    List<BodyType> findDistinctBodyTypes();
}