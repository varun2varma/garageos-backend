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

    List<EstimateItemResponse> getItemsByComplaint(Long complaintId);

    EstimateItemResponse updateItem(
            Long id,
            CreateEstimateItemRequest request);

    void deleteItem(Long id);

    /**
     * Mission: customer select/deselect of individual estimate items,
     * before approval. Customer-only, for an item on their own estimate,
     * only while the estimate has not yet been approved - see
     * EstimateItemServiceImpl.setItemSelection for the exact checks.
     */
    EstimateItemResponse setItemSelection(Long itemId, boolean selected);

}