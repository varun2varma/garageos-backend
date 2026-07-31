package com.garageos.modules.inspectionmaster.importer;

import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import com.garageos.core.loader.AbstractImporter;
import com.garageos.core.loader.CsvLoader;
import com.garageos.modules.inspectionmaster.entity.InspectionMaster;
import com.garageos.modules.inspectionmaster.repository.InspectionMasterRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InspectionMasterImporterImpl
        extends AbstractImporter<InspectionMaster>
        implements InspectionMasterImporter {

    private final InspectionMasterRepository repository;

    public InspectionMasterImporterImpl(
            CsvLoader csvLoader,
            EntityManager entityManager,
            InspectionMasterRepository repository) {

        super(
                csvLoader,
                entityManager,
                repository,
                "Inspection Master");

        this.repository = repository;
    }

    @Override
    @Transactional
    public void importMasters() {

        log.info("Starting Inspection Master Import...");

        Map<String, InspectionMaster> existingMasters =
                repository.findAll()
                        .stream()
                        .collect(Collectors.toMap(

                                master ->
                                        (
                                                master.getMake()
                                                        + ":"
                                                        + master.getModel()
                                                        + ":"
                                                        + master.getVariant()
                                                        + ":"
                                                        + master.getFuelType()
                                                        + ":"
                                                        + master.getTransmissionType()
                                                        + ":"
                                                        + master.getMinYear()
                                                        + ":"
                                                        + master.getMaxYear()
                                                        + ":"
                                                        + master.getMinOdometer()
                                                        + ":"
                                                        + master.getMaxOdometer()

                                        ).toLowerCase(),

                                Function.identity()

                        ));

        csvLoader.read(
                "inspectionmaster/inspection_master.csv",
                row -> {

                    rowRead();

                    String key =
                            (
                                    row.string("make")
                                            + ":"
                                            + row.string("model")
                                            + ":"
                                            + row.string("variant")
                                            + ":"
                                            + row.enumValue(
                                            FuelType.class,
                                            "fuelType")
                                            + ":"
                                            + row.enumValue(
                                            TransmissionType.class,
                                            "transmissionType")
                                            + ":"
                                            + row.integer("minYear")
                                            + ":"
                                            + row.integer("maxYear")
                                            + ":"
                                            + row.integer("minOdometer")
                                            + ":"
                                            + row.integer("maxOdometer")
                            ).toLowerCase();

                    if (existingMasters.containsKey(key)) {

                        skip();

                        return;
                    }

                    InspectionMaster master =
                            new InspectionMaster();

                    master.setMake(
                            row.string("make"));

                    master.setModel(
                            row.string("model"));

                    master.setVariant(
                            row.string("variant"));

                    master.setFuelType(
                            row.enumValue(
                                    FuelType.class,
                                    "fuelType"));

                    master.setTransmissionType(
                            row.enumValue(
                                    TransmissionType.class,
                                    "transmissionType"));

                    master.setMinYear(
                            row.integer("minYear"));

                    master.setMaxYear(
                            row.integer("maxYear"));

                    master.setMinOdometer(
                            row.integer("minOdometer"));

                    master.setMaxOdometer(
                            row.integer("maxOdometer"));

                    master.setActive(true);

                    write(master);

                    existingMasters.put(
                            key,
                            master);

                });

        finish();

        log.info("Inspection Master Import Completed.");

    }

}