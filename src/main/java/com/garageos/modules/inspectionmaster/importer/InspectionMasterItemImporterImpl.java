package com.garageos.modules.inspectionmaster.importer;

import com.garageos.core.loader.CsvLoader;
import com.garageos.core.enums.InspectionPriority;
import com.garageos.modules.inspectionmaster.entity.InspectionMaster;
import com.garageos.modules.inspectionmaster.entity.InspectionMasterItem;
import com.garageos.modules.inspectionmaster.repository.InspectionMasterItemRepository;
import com.garageos.modules.inspectionmaster.repository.InspectionMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class InspectionMasterItemImporterImpl
        implements InspectionMasterItemImporter {

    private final CsvLoader loader;

    private final InspectionMasterRepository masterRepository;

    private final InspectionMasterItemRepository itemRepository;

    @Override
    public void importItems() {

        log.info("Starting Inspection Master Item Import...");

        List<String[]> rows =
                loader.read("inspectionmaster/inspection_master_items.csv");

        Map<String, InspectionMaster> masterMap =
                masterRepository.findAll()
                        .stream()
                        .collect(Collectors.toMap(

                                master -> (

                                        master.getMake()
                                                + ":"
                                                + master.getModel()
                                                + ":"
                                                + master.getVariant()
                                                + ":"
                                                + master.getMinOdometer()
                                                + ":"
                                                + master.getMaxOdometer()

                                ).toLowerCase(),

                                Function.identity()
                        ));

        Set<String> existingItems =
                itemRepository.findAll()
                        .stream()
                        .map(item -> (

                                item.getInspectionMaster().getId()
                                        + ":"
                                        + item.getCheckItem()

                        ).toLowerCase())
                        .collect(Collectors.toSet());

        List<InspectionMasterItem> itemsToSave =
                new ArrayList<>();

        for (String[] row : rows) {

            String masterKey = (

                    row[0].trim()
                            + ":"
                            + row[1].trim()
                            + ":"
                            + row[2].trim()
                            + ":"
                            + row[3].trim()
                            + ":"
                            + row[4].trim()

            ).toLowerCase();

            InspectionMaster master =
                    masterMap.get(masterKey);

            if (master == null) {

                log.warn("Master not found : {}", masterKey);

                continue;
            }

            String duplicateKey =
                    (
                            master.getId()
                                    + ":"
                                    + row[6].trim()
                    ).toLowerCase();

            if (existingItems.contains(duplicateKey)) {
                continue;
            }

            InspectionMasterItem item =
                    new InspectionMasterItem();

            item.setInspectionMaster(master);

            item.setCategory(row[5].trim());

            item.setCheckItem(row[6].trim());

            item.setDescription(row[7].trim());

            item.setPriority(
                    InspectionPriority.valueOf(row[8].trim()));

            item.setMandatory(
                    Boolean.parseBoolean(row[9].trim()));

            item.setDisplayOrder(
                    Integer.parseInt(row[10].trim()));

            item.setServiceName(
                    row[11].trim());

            item.setServiceDescription(
                    row[12].trim());

            item.setLabourCost(
                    new BigDecimal(row[13].trim()));

            item.setPartCost(
                    new BigDecimal(row[14].trim()));

            item.setEstimatedLabourHours(
                    new BigDecimal(row[15].trim()));

            itemsToSave.add(item);

            existingItems.add(duplicateKey);
        }

        if (!itemsToSave.isEmpty()) {

            itemRepository.saveAll(itemsToSave);

            log.info("{} Inspection Master Items Imported.",
                    itemsToSave.size());

        } else {

            log.info("No Inspection Master Items to import.");
        }

        log.info("Inspection Master Item Import Completed.");
    }
}