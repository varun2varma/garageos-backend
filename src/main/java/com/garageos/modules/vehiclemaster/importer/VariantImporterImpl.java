package com.garageos.modules.vehiclemaster.importer;

import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import com.garageos.core.loader.AbstractImporter;
import com.garageos.core.loader.CsvLoader;
import com.garageos.modules.vehiclemaster.entity.VehicleModel;
import com.garageos.modules.vehiclemaster.entity.VehicleVariant;
import com.garageos.modules.vehiclemaster.repository.VehicleModelRepository;
import com.garageos.modules.vehiclemaster.repository.VehicleVariantRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class VariantImporterImpl
        extends AbstractImporter<VehicleVariant>
        implements VariantImporter {

    private final VehicleModelRepository modelRepository;
    private final VehicleVariantRepository variantRepository;

    public VariantImporterImpl(
            CsvLoader csvLoader,
            EntityManager entityManager,
            VehicleModelRepository modelRepository,
            VehicleVariantRepository variantRepository) {

        super(
                csvLoader,
                entityManager,
                variantRepository,
                "Vehicle Variant");

        this.modelRepository = modelRepository;
        this.variantRepository = variantRepository;
    }

    @Override
    @Transactional
    public void importVariants() {

        log.info("Starting Vehicle Variant Import...");

        Map<String, VehicleModel> modelMap =
                modelRepository.findAllWithBrand()
                        .stream()
                        .collect(Collectors.toMap(
                                model ->
                                        (
                                                model.getBrand().getName()
                                                        + ":"
                                                        + model.getName()
                                        ).toLowerCase(),
                                Function.identity()
                        ));

        Map<String, VehicleVariant> variantMap =
                variantRepository.findAll()
                        .stream()
                        .collect(Collectors.toMap(
                                variant ->
                                        (
                                                variant.getModel().getId()
                                                        + ":"
                                                        + variant.getVariantName()
                                                        + ":"
                                                        + variant.getFuelType()
                                                        + ":"
                                                        + variant.getTransmissionType()
                                        ).toLowerCase(),
                                Function.identity()
                        ));

        csvLoader.read(
                "vehiclemaster/variants.csv",
                row -> {

                    rowRead();

                    String modelKey =
                            (
                                    row.string("brand")
                                            + ":"
                                            + row.string("model")
                            ).toLowerCase();

                    VehicleModel model =
                            modelMap.get(modelKey);

                    if (model == null) {

                        fail();

                        log.warn(
                                "Model not found : {}",
                                modelKey);

                        return;
                    }

                    FuelType fuelType =
                            row.enumValue(
                                    FuelType.class,
                                    "fuel");

                    TransmissionType transmissionType =
                            row.enumValue(
                                    TransmissionType.class,
                                    "transmission");

                    String variantKey =
                            (
                                    model.getId()
                                            + ":"
                                            + row.string("variant")
                                            + ":"
                                            + fuelType
                                            + ":"
                                            + transmissionType
                            ).toLowerCase();

                    if (variantMap.containsKey(variantKey)) {

                        skip();

                        return;
                    }

                    VehicleVariant variant =
                            new VehicleVariant();

                    variant.setModel(model);
                    variant.setVariantName(
                            row.string("variant"));
                    variant.setFuelType(fuelType);
                    variant.setTransmissionType(transmissionType);

                    variant.setEngineCc(
                            row.integer("engineCc"));

                    variant.setHorsepower(
                            row.doubleValue("hp"));

                    variant.setTorqueNm(
                            row.doubleValue("torque"));

                    variant.setLaunchYear(
                            row.integer("launchYear"));

                    variant.setDiscontinuedYear(
                            row.integer("discontinuedYear"));

                    variant.setServiceIntervalKm(
                            row.integer("serviceKm"));

                    variant.setServiceIntervalMonths(
                            row.integer("serviceMonths"));

                    write(variant);

                    variantMap.put(
                            variantKey,
                            variant);

                });

        finish();

        log.info("Vehicle Variant Import Completed.");

    }

}