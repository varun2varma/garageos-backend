package com.garageos.modules.vehiclemaster.service.impl;

import com.garageos.core.enums.EnumDropdownResponse;
import com.garageos.core.enums.TransmissionType;
import com.garageos.core.exception.ResourceAlreadyExistsException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.vehiclemaster.dto.request.CreateVehicleVariantRequest;
import com.garageos.modules.vehiclemaster.dto.response.VehicleDropdownResponse;
import com.garageos.modules.vehiclemaster.dto.response.VehicleMasterMetadataResponse;
import com.garageos.modules.vehiclemaster.dto.response.VehicleVariantResponse;
import com.garageos.modules.vehiclemaster.entity.VehicleModel;
import com.garageos.modules.vehiclemaster.entity.VehicleVariant;
import com.garageos.modules.vehiclemaster.mapper.VehicleMasterMapper;
import com.garageos.modules.vehiclemaster.repository.VehicleModelRepository;
import com.garageos.modules.vehiclemaster.repository.VehicleVariantRepository;
import com.garageos.modules.vehiclemaster.service.VariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.garageos.core.enums.FuelType;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VariantServiceImpl implements VariantService {

    private final VehicleModelRepository modelRepository;

    private final VehicleVariantRepository variantRepository;

    private final VehicleMasterMapper mapper;


    // =========================================================
    // CREATE
    // =========================================================

    @Override
    public VehicleVariantResponse create(
            CreateVehicleVariantRequest request) {

        VehicleModel model =
                findModel(request.getModelId());

        if (variantRepository
                .findByModelAndVariantNameIgnoreCaseAndFuelTypeAndTransmissionType(
                        model,
                        request.getVariantName(),
                        request.getFuelType(),
                        request.getTransmissionType()
                )
                .isPresent()) {

            throw new ResourceAlreadyExistsException(
                    "Vehicle variant already exists."
            );
        }

        VehicleVariant variant =
                mapper.toEntity(request);

        variant.setModel(model);

        return mapper.toVariantResponse(
                variantRepository.save(variant)
        );
    }


    // =========================================================
    // GET BY ID
    // =========================================================

    @Override
    public VehicleVariantResponse getById(
            Long id) {

        return mapper.toVariantResponse(
                findVariant(id)
        );
    }


    // =========================================================
    // GET BY MODEL
    // =========================================================

    @Override
    public List<VehicleVariantResponse> getByModel(
            Long modelId) {

        VehicleModel model =
                findModel(modelId);

        return mapper.toVariantResponseList(
                variantRepository
                        .findByModelOrderByVariantNameAsc(model.getId())
        );
    }


    // =========================================================
    // UPDATE
    // =========================================================

    @Override
    public VehicleVariantResponse update(
            Long id,
            CreateVehicleVariantRequest request) {

        VehicleVariant variant =
                findVariant(id);

        VehicleModel model =
                findModel(request.getModelId());

        mapper.updateVariant(
                request,
                variant
        );

        variant.setModel(model);

        return mapper.toVariantResponse(
                variantRepository.save(variant)
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Override
    public void delete(Long id) {

        variantRepository.delete(
                findVariant(id)
        );
    }


    // =========================================================
    // FIND MODEL
    // =========================================================

    private VehicleModel findModel(Long id) {

        return modelRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle model not found."
                        )
                );
    }


    // =========================================================
    // FIND VARIANT
    // =========================================================

    private VehicleVariant findVariant(Long id) {

        return variantRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle variant not found."
                        )
                );
    }


    // =========================================================
    // VARIANT DROPDOWN
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<VehicleDropdownResponse> getVariantsByModel(
            Long modelId) {

        return variantRepository
                .findByModelIdOrderByVariantNameAsc(modelId)
                .stream()
                .map(variant ->
                        VehicleDropdownResponse.builder()
                                .id(variant.getId())
                                .name(variant.getVariantName())
                                .build()
                )
                .toList();
    }


    // =========================================================
    // METADATA
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public VehicleMasterMetadataResponse getMetadata() {

        return VehicleMasterMetadataResponse.builder()
                .fuelTypes(
                        variantRepository
                                .findDistinctFuelTypes()
                )
                .transmissionTypes(
                        variantRepository
                                .findDistinctTransmissionTypes()
                )
                .bodyTypes(
                        modelRepository
                                .findDistinctBodyTypes()
                )
                .build();
    }
// =========================================================
// MODEL + VARIANT → FUEL TYPE
// =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<EnumDropdownResponse> getFuelTypeDropdown(
            Long modelId,
            Long variantId) {

        return variantRepository
                .findDistinctFuelTypesByModelIdAndVariantId(
                        modelId,
                        variantId
                )
                .stream()
                .map(type ->
                        EnumDropdownResponse.builder()
                                .id(type.name())
                                .name(type.name())
                                .build()
                )
                .toList();
    }


// =========================================================
// MODEL + VARIANT + FUEL TYPE → TRANSMISSION
// =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<EnumDropdownResponse> getTransmissionDropdown(
            Long modelId,
            Long variantId,
            FuelType fuelType) {

        return variantRepository
                .findDistinctTransmissionTypesByModelIdAndVariantIdAndFuelType(
                        modelId,
                        variantId,
                        fuelType
                )
                .stream()
                .map(type ->
                        EnumDropdownResponse.builder()
                                .id(type.name())
                                .name(type.name())
                                .build()
                )
                .toList();
    }
}