package com.garageos.modules.catalog.service;

import com.garageos.modules.catalog.dto.request.PromoteManualItemRequest;
import com.garageos.modules.catalog.dto.response.ReviewItemResponse;
import org.springframework.data.domain.Page;

/**
 * Platform-level review of manually entered estimate lines. Resolving a line
 * links it to a catalog item; it never edits the line's description, quantity,
 * unit or price, and never touches an issued invoice.
 */
public interface CatalogReviewService {

    Page<ReviewItemResponse> pending(int page, int size);

    /** Link a manual line to an existing catalog item of the matching kind. */
    ReviewItemResponse link(Long estimateItemId, Long catalogItemId);

    /**
     * Create a catalog item from a manual line, or reuse the existing item with
     * the same type and normalised name, then link it. Never creates a duplicate.
     */
    ReviewItemResponse promote(Long estimateItemId, PromoteManualItemRequest request);
}
