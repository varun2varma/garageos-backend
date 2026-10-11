package com.garageos.modules.catalog.service.impl;

import com.garageos.core.enums.EstimateItemType;
import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.catalog.CatalogItemType;
import com.garageos.core.enums.catalog.ClassificationStatus;
import com.garageos.core.enums.catalog.ReviewStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ConflictException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.security.GarageScope;
import com.garageos.modules.catalog.dto.request.PromoteManualItemRequest;
import com.garageos.modules.catalog.dto.response.ReviewItemResponse;
import com.garageos.modules.catalog.entity.CatalogItem;
import com.garageos.modules.catalog.repository.CatalogItemRepository;
import com.garageos.modules.catalog.service.CatalogReviewService;
import com.garageos.modules.catalog.service.CatalogService;
import com.garageos.modules.catalog.service.CatalogUnits;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.estimate.repository.EstimateRepository;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.estimateitem.repository.EstimateItemRepository;
import com.garageos.modules.estimateitem.tax.EstimateTotalsCalculator;
import com.garageos.modules.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CatalogReviewServiceImpl implements CatalogReviewService {

    private final EstimateItemRepository estimateItemRepository;
    private final EstimateRepository estimateRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final CatalogService catalogService;
    private final InvoiceRepository invoiceRepository;
    private final EstimateTotalsCalculator totalsCalculator;
    private final GarageScope garageScope;

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewItemResponse> pending(int page, int size) {
        requireReviewer();
        return estimateItemRepository
                .findByReviewStatusOrderByIdAsc(
                        ReviewStatus.NEEDS_REVIEW,
                        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)))
                .map(CatalogReviewServiceImpl::toResponse);
    }

    @Override
    @Transactional
    public ReviewItemResponse link(Long estimateItemId, Long catalogItemId) {

        requireReviewer();

        lockEstimateOf(estimateItemId);

        EstimateItem line = requirePending(estimateItemId);

        requireOpenEstimate(line);

        CatalogItem catalogItem = catalogService.requireUsable(catalogItemId, expectedType(line));

        return resolve(line, catalogItem);
    }

    @Override
    @Transactional
    public ReviewItemResponse promote(Long estimateItemId, PromoteManualItemRequest request) {

        requireReviewer();

        lockEstimateOf(estimateItemId);

        EstimateItem line = requirePending(estimateItemId);

        requireOpenEstimate(line);

        CatalogItemType type = expectedType(line);

        String name = request.getName() == null || request.getName().isBlank()
                ? line.getDescription()
                : request.getName();

        String normalized = CatalogService.normalize(name);

        CatalogItem catalogItem = catalogItemRepository
                .findByItemTypeAndNameNormalized(type, normalized)
                .orElse(null);

        if (catalogItem != null) {
            // Never a duplicate: an inactive twin is a reviewer decision, not something to bypass.
            return resolve(line, catalogService.requireUsable(catalogItem.getId(), type));
        }

        String unit = firstNonBlank(request.getDefaultUnit(), line.getUnit());

        if (unit == null) {
            throw new BusinessException("A default unit is required to create a catalog item.");
        }

        CatalogItem created = new CatalogItem();
        created.setItemType(type);
        created.setName(name.trim());
        created.setNameNormalized(normalized);
        created.setDescription(request.getDescription() == null || request.getDescription().isBlank()
                ? null
                : request.getDescription().trim());
        created.setDefaultUnit(CatalogUnits.normalize(unit));
        created.setCategory(request.getCategory() == null || request.getCategory().isBlank()
                ? null
                : request.getCategory().trim());
        created.setCreatedByUserId(garageScope.principal().getId());

        catalogService.applyClassification(
                created, request.getHsnSacCode(), request.getTaxRuleCode(),
                Boolean.TRUE.equals(request.getVerified()), request.getClassificationSource());

        return resolve(line, catalogItemRepository.save(created));
    }

    /**
     * A line is linked only while the estimate can still change nothing the customer agreed to: not approved and no
     * invoice. Otherwise nothing is written and the line stays in the queue, so a review item can never be shown as
     * resolved while its classification is not applied. An approved estimate without an invoice is corrected with
     * the audited classification-correction action; an invoiced one needs the accountant's credit-note/reissue
     * process.
     */
    private void requireOpenEstimate(EstimateItem line) {

        Estimate estimate = line.getEstimate();

        if (invoiceRepository.existsByEstimateId(estimate.getId())) {
            throw new ConflictException(
                    "This estimate already has an invoice, so its lines cannot be reclassified. Correcting an issued "
                            + "invoice needs the accountant-approved credit-note/reissue process.",
                    "ESTIMATE_INVOICED");
        }

        if (estimate.getStatus() == EstimateStatus.APPROVED) {
            throw new ConflictException(
                    "This estimate is already approved. Use the classification-correction action for the line "
                            + "(it needs a reason and must not change the approved tax).",
                    "ESTIMATE_APPROVED_USE_CORRECTION");
        }
    }

    private ReviewItemResponse resolve(EstimateItem line, CatalogItem catalogItem) {

        line.setCatalogItemId(catalogItem.getId());
        line.setReviewStatus(ReviewStatus.RESOLVED);

        Estimate estimate = line.getEstimate();

        line.setHsnSacCode(catalogItem.getHsnSacCode());
        line.setTaxRuleCode(catalogItem.getTaxRuleCode());
        line.setGstRatePercent(
                catalogItem.getClassificationStatus() == ClassificationStatus.VERIFIED
                        ? catalogService.resolveRate(catalogItem.getTaxRuleCode(), LocalDate.now()).orElse(null)
                        : null);
        if (line.getUnit() == null) {
            line.setUnit(catalogItem.getDefaultUnit());
        }

        estimateItemRepository.save(line);

        totalsCalculator.recalculate(estimate);

        return toResponse(line);
    }

    /**
     * Estimate row lock taken BEFORE the line is read, so the line, the estimate status and the invoice check below
     * are the committed state held under the lock (serialised with approval, edits, invoicing and correction).
     */
    private void lockEstimateOf(Long estimateItemId) {

        Long estimateId = estimateItemRepository.findEstimateIdById(estimateItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate item not found."));

        estimateRepository.findByIdForUpdate(estimateId)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate item not found."));
    }

    private EstimateItem requirePending(Long id) {

        EstimateItem line = estimateItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate item not found."));

        if (line.getReviewStatus() != ReviewStatus.NEEDS_REVIEW) {
            throw new BusinessException("This item is not waiting for review.");
        }

        return line;
    }

    private static CatalogItemType expectedType(EstimateItem line) {
        if (line.getItemType() == EstimateItemType.PART) {
            return CatalogItemType.PART;
        }
        if (line.getItemType() == EstimateItemType.LABOUR) {
            return CatalogItemType.SERVICE;
        }
        throw new BusinessException("Other charges are not catalog items.");
    }

    /** Defence in depth behind the controller's SUPER_ADMIN rule. */
    private void requireReviewer() {
        if (!garageScope.isUnrestricted(garageScope.principal())) {
            throw new ResourceNotFoundException("Not found.");
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null || second.isBlank() ? null : second.trim();
    }

    static ReviewItemResponse toResponse(EstimateItem line) {
        return ReviewItemResponse.builder()
                .id(line.getId())
                .estimateId(line.getEstimate().getId())
                .garageId(line.getGarageId())
                .itemType(line.getItemType().name())
                .description(line.getDescription())
                .quantity(line.getQuantity())
                .unit(line.getUnit())
                .unitPrice(line.getUnitPrice())
                .manualNote(line.getManualNote())
                .createdByUserId(line.getCreatedByUserId())
                .createdAt(line.getCreatedAt())
                .reviewStatus(line.getReviewStatus().name())
                .catalogItemId(line.getCatalogItemId())
                .build();
    }
}
