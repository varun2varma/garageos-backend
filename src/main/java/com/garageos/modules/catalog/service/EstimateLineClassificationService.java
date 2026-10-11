package com.garageos.modules.catalog.service;

import com.garageos.modules.catalog.dto.request.CorrectEstimateLineClassificationRequest;
import com.garageos.modules.catalog.dto.response.ReviewItemResponse;

/**
 * Audited correction of the classification snapshot of one line of an APPROVED estimate that has no invoice.
 * Platform administrators only. Never changes quantity, unit, price, discount, totals or approval state.
 */
public interface EstimateLineClassificationService {

    ReviewItemResponse correct(Long estimateId, Long estimateItemId, CorrectEstimateLineClassificationRequest request);
}
