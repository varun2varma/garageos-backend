package com.garageos.modules.vehiclemaster.repository;

import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import com.garageos.modules.vehiclemaster.entity.VehicleModel;
import com.garageos.modules.vehiclemaster.entity.VehicleVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VehicleVariantRepository extends JpaRepository<VehicleVariant, Long> {

    List<VehicleVariant> findByModelOrderByVariantNameAsc(
            VehicleModel model
    );

    Optional<VehicleVariant> findByModelAndVariantNameIgnoreCaseAndFuelTypeAndTransmissionType(
            VehicleModel model,
            String variantName,
            FuelType fuelType,
            TransmissionType transmissionType
    );

    List<VehicleVariant> findByModelIdOrderByVariantNameAsc(Long modelId);

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

    @Query("""
    select distinct v.transmissionType
    from VehicleVariant v
    where v.model.id = :modelId
    order by v.transmissionType
    """)
    List<TransmissionType> findDistinctTransmissionTypesByModelId(Long modelId);

    @Query("""
    select distinct v.fuelType
    from VehicleVariant v
    where v.model.id = :modelId
      and v.transmissionType = :transmissionType
    order by v.fuelType
    """)
    List<FuelType> findDistinctFuelTypesByModelIdAndTransmissionType(
            Long modelId,
            TransmissionType transmissionType);

}