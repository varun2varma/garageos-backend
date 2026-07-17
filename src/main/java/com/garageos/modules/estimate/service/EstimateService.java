package com.garageos.modules.estimate.service;

import com.garageos.modules.estimate.dto.request.CreateEstimateRequest;
import com.garageos.modules.estimate.dto.response.EstimateResponse;

import java.util.List;

public interface EstimateService {

    EstimateResponse createEstimate(
            CreateEstimateRequest request);

    EstimateResponse getEstimate(Long id);

    List<EstimateResponse> getAllEstimates();

    EstimateResponse updateEstimate(
            Long id,
            CreateEstimateRequest request);

    void deleteEstimate(Long id);
    EstimateResponse approveEstimate(Long id);

    EstimateResponse rejectEstimate(Long id);
    EstimateResponse createEstimate(String jobCardNumber);
    EstimateResponse approveEstimate(String jobCardNumber);
}