package com.garageos.modules.vehiclemaster.importer;

import com.garageos.modules.vehiclemaster.entity.VehicleBrand;
import com.garageos.modules.vehiclemaster.entity.VehicleModel;
import com.garageos.core.loader.CsvLoader;
import com.garageos.modules.vehiclemaster.enums.BodyType;
import com.garageos.modules.vehiclemaster.repository.VehicleBrandRepository;
import com.garageos.modules.vehiclemaster.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelImporterImpl implements ModelImporter {

    private final CsvLoader loader;
    private final VehicleBrandRepository brandRepository;
    private final VehicleModelRepository modelRepository;

    @Override
    public void importModels() {

        var rows = loader.read("vehiclemaster/models.csv");

        for (String[] row : rows) {

            VehicleBrand brand = brandRepository
                    .findByNameIgnoreCase(row[0])
                    .orElseThrow();

            if (modelRepository.existsByBrandAndNameIgnoreCase(
                    brand,
                    row[1])) {

                continue;
            }

            VehicleModel model = new VehicleModel();

            model.setBrand(brand);
            model.setName(row[1]);
            model.setBodyType(BodyType.valueOf(row[2]));
            model.setSeatingCapacity(Integer.parseInt(row[3]));

            modelRepository.save(model);
        }

        log.info("Vehicle Models Imported");
    }
}