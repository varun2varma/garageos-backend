package com.garageos.modules.navigation.service;

import com.garageos.modules.navigation.dto.DriverLocationRequest;

public interface DriverLocationService {

    void processLocation(DriverLocationRequest request);
}