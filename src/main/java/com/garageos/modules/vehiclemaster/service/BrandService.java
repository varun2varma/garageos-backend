package com.garageos.modules.vehiclemaster.service;

import com.garageos.modules.vehiclemaster.dto.request.CreateVehicleBrandRequest;
import com.garageos.modules.vehiclemaster.dto.response.VehicleBrandResponse;
import com.garageos.modules.vehiclemaster.dto.response.VehicleDropdownResponse;

import java.util.List;

public interface BrandService {

    VehicleBrandResponse create(CreateVehicleBrandRequest request);

    VehicleBrandResponse getById(Long id);

    List<VehicleBrandResponse> getAll();

    VehicleBrandResponse update(Long id,
                                CreateVehicleBrandRequest request);

    void delete(Long id);

    List<VehicleDropdownResponse> getAllBrands();

}