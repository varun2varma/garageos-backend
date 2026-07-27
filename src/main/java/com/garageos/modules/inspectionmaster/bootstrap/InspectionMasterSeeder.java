package com.garageos.modules.inspectionmaster.bootstrap;

import com.garageos.modules.inspectionmaster.importer.InspectionMasterImporter;
import com.garageos.modules.inspectionmaster.importer.InspectionMasterItemImporter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InspectionMasterSeeder {

    private final InspectionMasterImporter masterImporter;
    private final InspectionMasterItemImporter itemImporter;

    @PostConstruct
    public void init() {

        log.info("==========================================");
        log.info("Starting Inspection Master Import");
        log.info("==========================================");

        masterImporter.importMasters();

        itemImporter.importItems();

        log.info("==========================================");
        log.info("Inspection Master Import Completed");
        log.info("==========================================");
    }
}