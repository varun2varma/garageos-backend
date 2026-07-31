package com.garageos.modules.inspectionmaster.importer;

import com.garageos.core.enums.InspectionPriority;
import com.garageos.core.loader.AbstractImporter;
import com.garageos.core.loader.CsvLoader;
import com.garageos.modules.inspectionmaster.entity.InspectionMaster;
import com.garageos.modules.inspectionmaster.entity.InspectionMasterItem;
import com.garageos.modules.inspectionmaster.repository.InspectionMasterItemRepository;
import com.garageos.modules.inspectionmaster.repository.InspectionMasterRepository;
import com.garageos.modules.inspectionmaster.repository.projection.InspectionMasterItemKey;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InspectionMasterItemImporterImpl
        extends AbstractImporter<InspectionMasterItem>
        implements InspectionMasterItemImporter {

    private final InspectionMasterRepository masterRepository;
    private final InspectionMasterItemRepository itemRepository;

    public InspectionMasterItemImporterImpl(
            CsvLoader csvLoader,
            EntityManager entityManager,
            InspectionMasterRepository masterRepository,
            InspectionMasterItemRepository itemRepository) {

        super(
                csvLoader,
                entityManager,
                itemRepository,
                "Inspection Master Item");

        this.masterRepository = masterRepository;
        this.itemRepository = itemRepository;
    }

    @Override
//    @Transactional
    public void importItems() {

        log.info("Starting Inspection Master Item Import...");

        Map<String, InspectionMaster> masterMap =
                masterRepository.findAll()
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
                                                        + master.getMinOdometer()
                                                        + ":"
                                                        + master.getMaxOdometer()

                                        ).toLowerCase(),

                                Function.identity()

                        ));

//        Set<String> existingItems =
//                itemRepository.findAllKeys()
//                        .stream()
//                        .map(key ->
//                                (
//                                        key.getInspectionMasterId()
//                                                + ":"
//                                                + key.getCheckItem()
//                                ).toLowerCase())
//                        .collect(Collectors.toSet());

        csvLoader.read(
                "inspectionmaster/inspection_master_items.csv",
                row -> {

                    rowRead();

                    String masterKey =
                            (
                                    row.string("make")
                                            + ":"
                                            + row.string("model")
                                            + ":"
                                            + row.string("variant")
                                            + ":"
                                            + row.integer("minOdometer")
                                            + ":"
                                            + row.integer("maxOdometer")
                            ).toLowerCase();

                    InspectionMaster master =
                            masterMap.get(masterKey);

                    if (master == null) {

                        fail();

                        log.warn(
                                "Master not found : {}",
                                masterKey);

                        return;
                    }

                    String duplicateKey =
                            (
                                    master.getId()
                                            + ":"
                                            + row.string("checkItem")
                            ).toLowerCase();

//                    if (existingItems.contains(duplicateKey)) {
//
//                        skip();
//
//                        return;
//                    }

                    InspectionMasterItem item =
                            new InspectionMasterItem();

                    item.setInspectionMaster(master);

                    item.setCategory(
                            row.string("category"));

                    item.setCheckItem(
                            row.string("checkItem"));

                    item.setDescription(
                            row.string("description"));

                    item.setPriority(
                            row.enumValue(
                                    InspectionPriority.class,
                                    "priority"));

                    item.setMandatory(
                            row.bool("mandatory"));

                    item.setDisplayOrder(
                            row.integer("displayOrder"));

                    item.setServiceName(
                            row.string("serviceName"));

                    item.setServiceDescription(
                            row.string("serviceDescription"));

                    item.setLabourCost(
                            row.decimal("labourCost"));

                    item.setPartCost(
                            row.decimal("partCost"));

                    item.setEstimatedLabourHours(
                            row.decimal("estimatedLabourHours"));

                    write(item);

//                    existingItems.add(duplicateKey);

                });

        finish();

        log.info("Inspection Master Item Import Completed.");

    }

}