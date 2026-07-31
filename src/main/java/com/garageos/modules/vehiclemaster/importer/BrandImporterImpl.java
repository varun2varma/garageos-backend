package com.garageos.modules.vehiclemaster.importer;

import com.garageos.core.loader.AbstractImporter;
import com.garageos.core.loader.CsvLoader;
import com.garageos.modules.vehiclemaster.entity.VehicleBrand;
import com.garageos.modules.vehiclemaster.repository.VehicleBrandRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BrandImporterImpl extends AbstractImporter<VehicleBrand>
        implements BrandImporter {

    private final VehicleBrandRepository repository;

    public BrandImporterImpl(
            CsvLoader csvLoader,
            EntityManager entityManager,
            VehicleBrandRepository repository) {

        super(
                csvLoader,
                entityManager,
                repository,
                "Vehicle Brand");

        this.repository = repository;
    }

    @Override
    public void importBrands() {

        Set<String> existingBrands =
                repository.findAll()
                        .stream()
                        .map(VehicleBrand::getName)
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());

        csvLoader.read(
                "vehiclemaster/brands.csv",
                row -> {

                    rowRead();

                    String name = row.string("name");

                    if (existingBrands.contains(name.toLowerCase())) {

                        skip();

                        return;

                    }

                    VehicleBrand brand = new VehicleBrand();

                    brand.setName(name);
                    brand.setCountry(row.string("country"));

                    write(brand);

                    existingBrands.add(name.toLowerCase());

                });

        finish();

    }

}