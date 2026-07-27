package com.garageos.modules.vehiclemaster.importer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VehicleMasterImporterImpl implements VehicleMasterImporter {

    private final BrandImporter brandImporter;
    private final ModelImporter modelImporter;
    private final VariantImporter variantImporter;

    @Override
    public void importData() {

        brandImporter.importBrands();

        modelImporter.importModels();

        variantImporter.importVariants();

    }
}