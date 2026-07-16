package com.garageos.modules.vehicle.service;

import com.garageos.modules.vehicle.dto.request.CreateVehicleRequest;
import com.garageos.modules.vehicle.dto.response.VehicleResponse;
import org.springframework.data.domain.Page;

public interface VehicleService {

    VehicleResponse createVehicle(CreateVehicleRequest request);

    VehicleResponse getVehicle(Long id);

    Page<VehicleResponse> getAllVehicles(
            int page,
            int size,
            String sortBy,
            String direction);

    VehicleResponse updateVehicle(
            Long id,
            CreateVehicleRequest request);

    void deleteVehicle(Long id);

    VehicleResponse getVehicleByRegistrationNumber(
            String registrationNumber);
}