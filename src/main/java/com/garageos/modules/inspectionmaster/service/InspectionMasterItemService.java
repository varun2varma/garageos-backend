package com.garageos.modules.inspectionmaster.service;

import com.garageos.modules.inspectionmaster.dto.request.CreateInspectionMasterItemRequest;
import com.garageos.modules.inspectionmaster.dto.response.InspectionMasterItemResponse;

import java.util.List;

public interface InspectionMasterItemService {

    InspectionMasterItemResponse createInspectionMasterItem(
            Long inspectionMasterId,
            CreateInspectionMasterItemRequest request);

    InspectionMasterItemResponse getInspectionMasterItem(
            Long id);

    List<InspectionMasterItemResponse> getInspectionMasterItems(
            Long inspectionMasterId);

    InspectionMasterItemResponse updateInspectionMasterItem(
            Long id,
            CreateInspectionMasterItemRequest request);

    void deleteInspectionMasterItem(
            Long id);

}