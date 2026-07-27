package com.garageos.modules.vehiclemaster.importer;

import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import com.garageos.modules.vehiclemaster.entity.VehicleBrand;
import com.garageos.modules.vehiclemaster.entity.VehicleModel;
import com.garageos.modules.vehiclemaster.entity.VehicleVariant;
import com.garageos.core.loader.CsvLoader;
import com.garageos.modules.vehiclemaster.repository.VehicleBrandRepository;
import com.garageos.modules.vehiclemaster.repository.VehicleModelRepository;
import com.garageos.modules.vehiclemaster.repository.VehicleVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class VariantImporterImpl implements VariantImporter {

    private final CsvLoader loader;
//    private final VehicleBrandRepository brandRepository;
    private final VehicleModelRepository modelRepository;
    private final VehicleVariantRepository variantRepository;

    @Override
    @Transactional
    public void importVariants() {

        log.info("Starting Variant Import...");

        List<String[]> rows = loader.read("vehiclemaster/variants.csv");

        // ----------------------------
        // Load Brands into Memory
        // ----------------------------

//        Map<String, VehicleBrand> brandMap =
//                brandRepository.findAll()
//                        .stream()
//                        .collect(Collectors.toMap(
//                                b -> b.getName().toLowerCase(),
//                                Function.identity()
//                        ));

        // ----------------------------
        // Load Models into Memory
        // ----------------------------

        Map<String, VehicleModel> modelMap =
                modelRepository.findAllWithBrand()
                        .stream()
                        .collect(Collectors.toMap(
                                m -> (m.getBrand().getName() + ":" + m.getName()).toLowerCase(),
                                Function.identity()
                        ));

        // ----------------------------
        // Load Existing Variants
        // ----------------------------

        Map<String, VehicleVariant> variantMap =
                variantRepository.findAll()
                        .stream()
                        .collect(Collectors.toMap(
                                v -> (
                                        v.getModel().getId()
                                                + ":"
                                                + v.getVariantName()
                                                + ":"
                                                + v.getFuelType()
                                                + ":"
                                                + v.getTransmissionType()
                                ).toLowerCase(),
                                Function.identity()
                        ));

        List<VehicleVariant> variantsToSave = new ArrayList<>();

        // ----------------------------
        // Import
        // ----------------------------

        for (String[] row : rows) {

            if (row.length < 12) {
                log.warn("Skipping invalid row: {}", Arrays.toString(row));
                continue;
            }

            String brandName = row[0].trim();
            String modelName = row[1].trim();

            String modelKey =
                    (brandName + ":" + modelName).toLowerCase();

            VehicleModel model = modelMap.get(modelKey);

            if (model == null) {

                log.warn("Skipping row. Model not found : {}", modelKey);

                continue;
            }

            FuelType fuelType = FuelType.valueOf(row[3].trim());
            TransmissionType transmissionType =
                    TransmissionType.valueOf(row[4].trim());

            String variantKey =
                    (
                            model.getId()
                                    + ":"
                                    + row[2].trim()
                                    + ":"
                                    + fuelType
                                    + ":"
                                    + transmissionType
                    ).toLowerCase();

            if (variantMap.containsKey(variantKey)) {

                log.debug("Variant already exists : {}", variantKey);

                continue;
            }

            VehicleVariant variant = new VehicleVariant();

            variant.setModel(model);
            variant.setVariantName(row[2].trim());
            variant.setFuelType(fuelType);
            variant.setTransmissionType(transmissionType);

            variant.setEngineCc(parseInteger(row[5]));
            variant.setHorsepower(parseDouble(row[6]));
            variant.setTorqueNm(parseDouble(row[7]));
            variant.setLaunchYear(parseInteger(row[8]));
            variant.setDiscontinuedYear(parseInteger(row[9]));
            variant.setServiceIntervalKm(parseInteger(row[10]));
            variant.setServiceIntervalMonths(parseInteger(row[11]));

            variantsToSave.add(variant);

            variantMap.put(variantKey, variant);
        }

        if (!variantsToSave.isEmpty()) {

            variantRepository.saveAll(variantsToSave);

            log.info("{} variants imported successfully.",
                    variantsToSave.size());

        } else {

            log.info("No new variants found.");
        }

        log.info("Variant Import Completed.");
    }

    private Integer parseInteger(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return Integer.parseInt(value.trim());
    }

    private Double parseDouble(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return Double.parseDouble(value.trim());
    }
}