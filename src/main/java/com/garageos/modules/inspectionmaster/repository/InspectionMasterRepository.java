package com.garageos.modules.inspectionmaster.repository;

import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import com.garageos.modules.inspectionmaster.entity.InspectionMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
      AND (
            (im.variant IS NULL AND :variant IS NULL)
            OR LOWER(im.variant) = LOWER(:variant)
      )
      AND im.fuelType = :fuelType
      AND im.transmissionType = :transmissionType
      AND :year BETWEEN im.minYear AND im.maxYear
      AND :odometer BETWEEN im.minOdometer AND im.maxOdometer
      AND im.active = true
    """)
    Optional<InspectionMaster> findApplicableInspectionMaster(
            String make,
            String model,
            String variant,
            FuelType fuelType,
            TransmissionType transmissionType,
            Integer year,
            Integer odometer
    );

}