package com.garageos.modules.vehiclemaster.service;

import com.garageos.modules.vehiclemaster.dto.request.CreateVehicleVariantRequest;
import com.garageos.modules.vehiclemaster.dto.response.VehicleDropdownResponse;
import com.garageos.modules.vehiclemaster.dto.response.VehicleMasterMetadataResponse;
import com.garageos.modules.vehiclemaster.dto.response.VehicleVariantResponse;

import java.util.List;

public interface VariantService {

    VehicleVariantResponse create(CreateVehicleVariantRequest request);

    VehicleVariantResponse getById(Long id);

    List<VehicleVariantResponse> getByModel(Long modelId);

    VehicleVariantResponse update(Long id,
                                  CreateVehicleVariantRequest request);

    void delete(Long id);

    List<VehicleDropdownResponse> getVariantsByModel(Long modelId);

    VehicleMasterMetadataResponse getMetadata();
}