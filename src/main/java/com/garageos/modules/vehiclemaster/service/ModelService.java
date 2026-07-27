package com.garageos.modules.vehiclemaster.service;

import com.garageos.modules.vehiclemaster.dto.request.CreateVehicleModelRequest;
import com.garageos.modules.vehiclemaster.dto.response.VehicleDropdownResponse;
import com.garageos.modules.vehiclemaster.dto.response.VehicleModelResponse;

import java.util.List;

public interface ModelService {

    VehicleModelResponse create(CreateVehicleModelRequest request);

    VehicleModelResponse getById(Long id);

    List<VehicleModelResponse> getByBrand(Long brandId);

    VehicleModelResponse update(Long id,
                                CreateVehicleModelRequest request);

    void delete(Long id);

    List<VehicleDropdownResponse> getModelsByBrand(Long brandId);

}