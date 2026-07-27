package com.garageos.modules.inspectionmaster.importer;

import com.garageos.core.loader.CsvLoader;
import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import com.garageos.modules.inspectionmaster.entity.InspectionMaster;
import com.garageos.modules.inspectionmaster.repository.InspectionMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class InspectionMasterImporterImpl implements InspectionMasterImporter {

    private final CsvLoader loader;
    private final InspectionMasterRepository repository;

    @Override
    public void importMasters() {

        log.info("Starting Inspection Master Import...");

        List<String[]> rows =
                loader.read("inspectionmaster/inspection_master.csv");

        Map<String, InspectionMaster> existingMasters =
                repository.findAll()
                        .stream()
                        .collect(Collectors.toMap(

                                master -> (
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

        List<InspectionMaster> mastersToSave = new ArrayList<>();

        for (String[] row : rows) {

            String key = (

                    row[0].trim()
                            + ":"
                            + row[1].trim()
                            + ":"
                            + row[2].trim()
                            + ":"
                            + row[3].trim()
                            + ":"
                            + row[4].trim()
                            + ":"
                            + row[5].trim()
                            + ":"
                            + row[6].trim()
                            + ":"
                            + row[7].trim()
                            + ":"
                            + row[8].trim()

            ).toLowerCase();

            if (existingMasters.containsKey(key)) {
                continue;
            }

            InspectionMaster master = new InspectionMaster();

            master.setMake(row[0].trim());
            master.setModel(row[1].trim());
            master.setVariant(row[2].trim());

            master.setFuelType(
                    FuelType.valueOf(row[3].trim()));

            master.setTransmissionType(
                    TransmissionType.valueOf(row[4].trim()));

            master.setMinYear(
                    Integer.parseInt(row[5]));

            master.setMaxYear(
                    Integer.parseInt(row[6]));

            master.setMinOdometer(
                    Integer.parseInt(row[7]));

            master.setMaxOdometer(
                    Integer.parseInt(row[8]));

            master.setActive(true);

            mastersToSave.add(master);

            existingMasters.put(key, master);
        }

        if (!mastersToSave.isEmpty()) {

            repository.saveAll(mastersToSave);

            log.info("{} Inspection Masters Imported.",
                    mastersToSave.size());

        } else {

            log.info("No Inspection Masters to import.");
        }
    }
}