package com.garageos.modules.inspectionfinding.service;

import com.garageos.modules.inspectionfinding.dto.request.CreateInspectionFindingRequest;
import com.garageos.modules.inspectionfinding.dto.response.InspectionFindingResponse;

import java.util.List;

public interface InspectionFindingService {

    InspectionFindingResponse createInspectionFinding(
            Long inspectionId,
            CreateInspectionFindingRequest request);

    InspectionFindingResponse getInspectionFinding(
            Long id);

    List<InspectionFindingResponse> getInspectionFindings(
            Long inspectionId);

    InspectionFindingResponse updateInspectionFinding(
            Long id,
            CreateInspectionFindingRequest request);

    void deleteInspectionFinding(
            Long id);

    void loadInspectionTemplate(
            Long inspectionId);

    void completeInspection(Long jobCardId);
}