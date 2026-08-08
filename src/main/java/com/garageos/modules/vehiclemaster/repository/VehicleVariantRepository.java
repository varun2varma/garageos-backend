package com.garageos.modules.vehiclemaster.repository;

import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import com.garageos.modules.vehiclemaster.entity.VehicleModel;
import com.garageos.modules.vehiclemaster.entity.VehicleVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VehicleVariantRepository
        extends JpaRepository<VehicleVariant, Long> {

    // =========================================================
    // Model → Variant
    // =========================================================

    List<VehicleVariant> findByModelOrderByVariantNameAsc(
            Long modelId
    );

    Optional<VehicleVariant>
    findByModelAndVariantNameIgnoreCaseAndFuelTypeAndTransmissionType(
            VehicleModel model,
            String variantName,
            FuelType fuelType,
            TransmissionType transmissionType
    );

    List<VehicleVariant> findByModelIdOrderByVariantNameAsc(
            Long modelId
    );


    // =========================================================
    // Metadata
    // =========================================================

    @Query("""
        SELECT DISTINCT v.fuelType
        FROM VehicleVariant v
        ORDER BY v.fuelType
        """)
    List<FuelType> findDistinctFuelTypes();


    @Query("""
        SELECT DISTINCT v.transmissionType
        FROM VehicleVariant v
        ORDER BY v.transmissionType
        """)
    List<TransmissionType> findDistinctTransmissionTypes();


    // =========================================================
    // Model + Variant → Fuel Type
    // =========================================================

    @Query("""
        SELECT DISTINCT v.fuelType
        FROM VehicleVariant v
        WHERE v.model.id = :modelId
          AND v.id = :variantId
        ORDER BY v.fuelType
        """)
    List<FuelType> findDistinctFuelTypesByModelIdAndVariantId(
            Long modelId,
            Long variantId
    );


    // =========================================================
    // Model + Variant + Fuel Type → Transmission
    // =========================================================

    @Query("""
        SELECT DISTINCT v.transmissionType
        FROM VehicleVariant v
        WHERE v.model.id = :modelId
          AND v.id = :variantId
          AND v.fuelType = :fuelType
        ORDER BY v.transmissionType
        """)
    List<TransmissionType>
    findDistinctTransmissionTypesByModelIdAndVariantIdAndFuelType(
            Long modelId,
            Long variantId,
            FuelType fuelType
    );
}