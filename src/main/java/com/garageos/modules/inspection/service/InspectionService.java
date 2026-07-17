package com.garageos.modules.inspection.service;

import com.garageos.modules.inspection.dto.request.CreateInspectionRequest;
import com.garageos.modules.inspection.dto.response.InspectionResponse;

import java.util.List;

public interface InspectionService {

    InspectionResponse createInspection(
            Long complaintId,
            CreateInspectionRequest request);

    InspectionResponse getInspection(Long id);

    List<InspectionResponse> getInspections(Long complaintId);

    InspectionResponse updateInspection(
            Long id,
            CreateInspectionRequest request);

    void deleteInspection(Long id);
    List<InspectionResponse> startInspection(String jobCardNumber);

    List<InspectionResponse> completeInspection(
            String jobCardNumber,
            List<CreateInspectionRequest> request);
}