package com.garageos.modules.inspectionmaster.service;

import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import com.garageos.modules.inspectionmaster.dto.request.CreateInspectionMasterRequest;
import com.garageos.modules.inspectionmaster.dto.response.InspectionMasterResponse;
import org.springframework.data.domain.Page;

public interface InspectionMasterService {

    InspectionMasterResponse createInspectionMaster(
            CreateInspectionMasterRequest request);

    InspectionMasterResponse getInspectionMaster(Long id);

    InspectionMasterResponse updateInspectionMaster(
            Long id,
            CreateInspectionMasterRequest request);

    void deleteInspectionMaster(Long id);

    Page<InspectionMasterResponse> getAllInspectionMasters(
            int page,
            int size,
            String sortBy,
            String direction);

    InspectionMasterResponse getInspectionMasterByVehicle(
            String make,
            String model,
            String variant,
            FuelType fuelType,
            TransmissionType transmissionType,
            Integer year,
            Integer odometer
    );

}