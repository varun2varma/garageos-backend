package com.garageos.modules.garage.service;

import com.garageos.modules.garage.dto.request.CreateGarageRequest;
import com.garageos.modules.garage.dto.response.GarageResponse;

public interface GarageService {

    GarageResponse createGarage(
            Long userId,
            CreateGarageRequest request
    );

    GarageResponse getGarage(Long id);

    GarageResponse updateGarage(
            Long id,
            CreateGarageRequest request
    );

    void deleteGarage(Long id);

}