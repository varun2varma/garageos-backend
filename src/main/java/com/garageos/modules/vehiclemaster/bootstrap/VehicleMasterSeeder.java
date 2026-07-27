package com.garageos.modules.vehiclemaster.bootstrap;

import com.garageos.modules.vehiclemaster.importer.VehicleMasterImporter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VehicleMasterSeeder {

    private final VehicleMasterImporter importer;

    @PostConstruct
    public void init() {

        importer.importData();

    }

}