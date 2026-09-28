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

    /**
     * Mission: the customer approval boundary is the Complaint, not the
     * individual EstimateItem - rejecting "AC not cooling" must reject all
     * of its parts/labour/other items together. Reuses the existing
     * `selected` field per item (no new boolean); also cancels any
     * RepairTask already created for this complaint when rejecting, per
     * the same "don't physically delete history" rule as everywhere else.
     */
    List<EstimateItemResponse> setComplaintItemsSelection(
            Long complaintId,
            boolean selected);

}