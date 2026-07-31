package com.garageos.modules.vehiclemaster.importer;

import com.garageos.core.loader.AbstractImporter;
import com.garageos.core.loader.CsvLoader;
import com.garageos.modules.vehiclemaster.entity.VehicleBrand;
import com.garageos.modules.vehiclemaster.entity.VehicleModel;
import com.garageos.modules.vehiclemaster.enums.BodyType;
import com.garageos.modules.vehiclemaster.repository.VehicleBrandRepository;
import com.garageos.modules.vehiclemaster.repository.VehicleModelRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ModelImporterImpl
        extends AbstractImporter<VehicleModel>
        implements ModelImporter {

    private final VehicleBrandRepository brandRepository;
    private final VehicleModelRepository modelRepository;

    public ModelImporterImpl(
            CsvLoader csvLoader,
            EntityManager entityManager,
            VehicleBrandRepository brandRepository,
            VehicleModelRepository repository) {

        super(
                csvLoader,
                entityManager,
                repository,
                "Vehicle Model");

        this.brandRepository = brandRepository;
        this.modelRepository = repository;
    }

    @Override
    public void importModels() {

//        Map<String, VehicleBrand> brands =
//                brandRepository.findAll()
//                        .stream()
//                        .collect(Collectors.toMap(
//                                b -> b.getName().toLowerCase(),
//                                Function.identity()
//                        ));
//
//        Set<String> existingModels =
//                brandRepository.findAll()
//                        .stream()
//                        .flatMap(b -> b.getModels().stream())
//                        .map(m -> (
//                                m.getBrand().getName()
//                                        + ":"
//                                        + m.getName()
//                        ).toLowerCase())
//                        .collect(Collectors.toSet());

        Map<String, VehicleBrand> brands =
                brandRepository.findAll()
                        .stream()
                        .collect(Collectors.toMap(
                                b -> b.getName().toLowerCase(),
                                Function.identity()
                        ));

        Set<String> existingModels =
                modelRepository.findAllWithBrand()
                        .stream()
                        .map(m ->
                                (m.getBrand().getName() + ":" + m.getName())
                                        .toLowerCase())
                        .collect(Collectors.toSet());

        csvLoader.read(
                "vehiclemaster/models.csv",
                row -> {

                    rowRead();

                    String brandName =
                            row.string("brand");

                    VehicleBrand brand =
                            brands.get(
                                    brandName.toLowerCase());

                    if (brand == null) {

                        fail();

                        log.warn(
                                "Brand not found : {}",
                                brandName);

                        return;

                    }

                    String key =
                            (
                                    brandName
                                            + ":"
                                            + row.string("name")
                            ).toLowerCase();

                    if (existingModels.contains(key)) {

                        skip();

                        return;

                    }

                    VehicleModel model =
                            new VehicleModel();

                    model.setBrand(brand);
                    model.setName(row.string("name"));
                    model.setBodyType(
                            row.enumValue(
                                    BodyType.class,
                                    "bodyType"));

                    model.setSeatingCapacity(
                            row.integer(
                                    "seatingCapacity"));

                    write(model);

                    existingModels.add(key);

                });

        finish();

    }

}