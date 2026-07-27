package com.garageos.modules.vehiclemaster.importer;

import com.garageos.modules.vehiclemaster.entity.VehicleBrand;
import com.garageos.core.loader.CsvLoader;
import com.garageos.modules.vehiclemaster.repository.VehicleBrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrandImporterImpl implements BrandImporter {

    private final CsvLoader loader;
    private final VehicleBrandRepository repository;

    @Override
    public void importBrands() {

        var rows = loader.read("vehiclemaster/brands.csv");

        for (String[] row : rows) {

            String name = row[0];
            String country = row[1];

            if (repository.existsByNameIgnoreCase(name)) {
                continue;
            }

            VehicleBrand brand = new VehicleBrand();

            brand.setName(name);
            brand.setCountry(country);

            repository.save(brand);
        }

        log.info("Vehicle brands imported.");
    }
}