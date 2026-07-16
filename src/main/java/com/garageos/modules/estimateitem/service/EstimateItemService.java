package com.garageos.modules.estimateitem.service;

import com.garageos.modules.estimateitem.dto.request.CreateEstimateItemRequest;
import com.garageos.modules.estimateitem.dto.response.EstimateItemResponse;

import java.util.List;

public interface EstimateItemService {

    EstimateItemResponse addItem(
            Long estimateId,
            CreateEstimateItemRequest request);

    EstimateItemResponse getItem(Long id);

    List<EstimateItemResponse> getItems(Long estimateId);

    EstimateItemResponse updateItem(
            Long id,
            CreateEstimateItemRequest request);

    void deleteItem(Long id);

}