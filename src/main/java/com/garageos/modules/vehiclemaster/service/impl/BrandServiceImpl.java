package com.garageos.modules.vehiclemaster.service.impl;

import com.garageos.core.exception.ResourceAlreadyExistsException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.vehiclemaster.dto.request.CreateVehicleBrandRequest;
import com.garageos.modules.vehiclemaster.dto.response.VehicleBrandResponse;
import com.garageos.modules.vehiclemaster.dto.response.VehicleDropdownResponse;
import com.garageos.modules.vehiclemaster.entity.VehicleBrand;
import com.garageos.modules.vehiclemaster.mapper.VehicleMasterMapper;
import com.garageos.modules.vehiclemaster.repository.VehicleBrandRepository;
import com.garageos.modules.vehiclemaster.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final VehicleBrandRepository repository;
    private final VehicleMasterMapper mapper;

    @Override
    public VehicleBrandResponse create(CreateVehicleBrandRequest request) {

        if (repository.existsByNameIgnoreCase(request.getName())) {
            throw new ResourceAlreadyExistsException(
                    "Vehicle brand already exists."
            );
        }

        VehicleBrand entity = mapper.toEntity(request);

        return mapper.toBrandResponse(
                repository.save(entity)
        );
    }

    @Override
    public VehicleBrandResponse getById(Long id) {

        return mapper.toBrandResponse(findEntity(id));
    }

    @Override
    public List<VehicleBrandResponse> getAll() {

        return mapper.toBrandResponseList(
                repository.findAll()
        );
    }

    @Override
    public VehicleBrandResponse update(Long id,
                                       CreateVehicleBrandRequest request) {

        VehicleBrand entity = findEntity(id);

        mapper.updateBrand(request, entity);

        return mapper.toBrandResponse(
                repository.save(entity)
        );
    }

    @Override
    public void delete(Long id) {

        repository.delete(findEntity(id));
    }

    private VehicleBrand findEntity(Long id) {

        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle brand not found."
                        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleDropdownResponse> getAllBrands() {

        return repository.findAllByOrderByNameAsc()
                .stream()
                .map(brand -> VehicleDropdownResponse.builder()
                        .id(brand.getId())
                        .name(brand.getName())
                        .build())
                .toList();
    }
}