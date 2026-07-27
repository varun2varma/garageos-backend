package com.garageos.modules.vehiclemaster.service;

import com.garageos.modules.vehiclemaster.dto.request.CreateVehicleBrandRequest;
import com.garageos.modules.vehiclemaster.dto.request.CreateVehicleModelRequest;
import com.garageos.modules.vehiclemaster.dto.request.CreateVehicleVariantRequest;
import com.garageos.modules.vehiclemaster.dto.response.VehicleBrandResponse;
import com.garageos.modules.vehiclemaster.dto.response.VehicleModelResponse;
import com.garageos.modules.vehiclemaster.dto.response.VehicleVariantResponse;

import java.util.List;

public interface VehicleMasterService {

    // Brand

    VehicleBrandResponse createBrand(CreateVehicleBrandRequest request);

    List<VehicleBrandResponse> getAllBrands();

    VehicleBrandResponse getBrand(Long id);

    VehicleBrandResponse updateBrand(Long id,
                                     CreateVehicleBrandRequest request);

    void deleteBrand(Long id);

    // Model

    VehicleModelResponse createModel(CreateVehicleModelRequest request);

    List<VehicleModelResponse> getModelsByBrand(Long brandId);

    VehicleModelResponse updateModel(Long id,
                                     CreateVehicleModelRequest request);

    void deleteModel(Long id);

    // Variant

    VehicleVariantResponse createVariant(CreateVehicleVariantRequest request);

    List<VehicleVariantResponse> getVariantsByModel(Long modelId);

    VehicleVariantResponse updateVariant(Long id,
                                         CreateVehicleVariantRequest request);

    void deleteVariant(Long id);

}