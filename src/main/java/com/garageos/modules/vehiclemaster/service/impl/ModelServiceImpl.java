package com.garageos.modules.vehiclemaster.service.impl;

import com.garageos.core.exception.ResourceAlreadyExistsException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.vehiclemaster.dto.request.CreateVehicleModelRequest;
import com.garageos.modules.vehiclemaster.dto.response.VehicleDropdownResponse;
import com.garageos.modules.vehiclemaster.dto.response.VehicleModelResponse;
import com.garageos.modules.vehiclemaster.entity.VehicleBrand;
import com.garageos.modules.vehiclemaster.entity.VehicleModel;
import com.garageos.modules.vehiclemaster.mapper.VehicleMasterMapper;
import com.garageos.modules.vehiclemaster.repository.VehicleBrandRepository;
import com.garageos.modules.vehiclemaster.repository.VehicleModelRepository;
import com.garageos.modules.vehiclemaster.service.ModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {

    private final VehicleBrandRepository brandRepository;
    private final VehicleModelRepository modelRepository;
    private final VehicleMasterMapper mapper;

    @Override
    public VehicleModelResponse create(CreateVehicleModelRequest request) {

        VehicleBrand brand = findBrand(request.getBrandId());

        if (modelRepository.existsByBrandAndNameIgnoreCase(
                brand,
                request.getName())) {

            throw new ResourceAlreadyExistsException(
                    "Vehicle model already exists."
            );
        }

        VehicleModel model = mapper.toEntity(request);
        model.setBrand(brand);

        return mapper.toModelResponse(
                modelRepository.save(model)
        );
    }

    @Override
    public VehicleModelResponse getById(Long id) {

        return mapper.toModelResponse(findModel(id));
    }

    @Override
    public List<VehicleModelResponse> getByBrand(Long brandId) {

        VehicleBrand brand = findBrand(brandId);

        return mapper.toModelResponseList(
                modelRepository.findByBrandOrderByNameAsc(brand)
        );
    }

    @Override
    public VehicleModelResponse update(Long id,
                                       CreateVehicleModelRequest request) {

        VehicleModel model = findModel(id);
        VehicleBrand brand = findBrand(request.getBrandId());

        mapper.updateModel(request, model);
        model.setBrand(brand);

        return mapper.toModelResponse(
                modelRepository.save(model)
        );
    }

    @Override
    public void delete(Long id) {

        modelRepository.delete(findModel(id));
    }

    private VehicleBrand findBrand(Long id) {

        return brandRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Vehicle brand not found."));
    }

    private VehicleModel findModel(Long id) {

        return modelRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Vehicle model not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleDropdownResponse> getModelsByBrand(Long brandId) {

        return modelRepository.findByBrandIdOrderByNameAsc(brandId)
                .stream()
                .map(model -> VehicleDropdownResponse.builder()
                        .id(model.getId())
                        .name(model.getName())
                        .build())
                .toList();
    }
}