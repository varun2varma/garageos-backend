package com.garageos.modules.inspectionmaster.repository;

import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import com.garageos.modules.inspectionmaster.entity.InspectionMaster;
import com.garageos.modules.inspectionmaster.repository.projection.InspectionMasterLookup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InspectionMasterRepository
        extends JpaRepository<InspectionMaster, Long> {

    Optional<InspectionMaster> findByMakeIgnoreCaseAndModelIgnoreCaseAndVariantIgnoreCaseAndFuelTypeAndTransmissionTypeAndActiveTrue(
            String make,
            String model,
            String variant,
            FuelType fuelType,
            TransmissionType transmissionType
    );

    @Query("""
    SELECT im
    FROM InspectionMaster im
    WHERE LOWER(im.make) = LOWER(:make)
      AND LOWER(im.model) = LOWER(:model)
      AND (:variant IS NULL OR LOWER(im.variant) = LOWER(:variant))
      AND im.fuelType = :fuelType
      AND im.transmissionType = :transmissionType
      AND :year BETWEEN im.minYear AND im.maxYear
      AND :odometer BETWEEN im.minOdometer AND im.maxOdometer
      AND im.active = true
    """)
    Optional<InspectionMaster> findApplicableInspectionMaster(
            @Param("make") String make,
            @Param("model") String model,
            @Param("variant") String variant,
            @Param("fuelType") FuelType fuelType,
            @Param("transmissionType") TransmissionType transmissionType,
            @Param("year") Integer year,
            @Param("odometer") Integer odometer
    );

    @Query("""
    SELECT
        m.id AS id,
        m.make AS make,
        m.model AS model,
        m.variant AS variant,
        m.fuelType AS fuelType,
        m.transmissionType AS transmissionType,
        m.minYear AS minYear,
        m.maxYear AS maxYear,
        m.minOdometer AS minOdometer,
        m.maxOdometer AS maxOdometer
    FROM InspectionMaster m
    """)
    List<InspectionMasterLookup> findAllLookup();

}