package com.garageos.modules.catalog.service.impl;

import com.garageos.core.enums.EstimateItemType;
import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.audit.AuditEventType;
import com.garageos.core.enums.catalog.CatalogItemType;
import com.garageos.core.enums.catalog.ClassificationStatus;
import com.garageos.core.enums.catalog.PriceTaxMode;
import com.garageos.core.enums.catalog.ReviewStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ConflictException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.enums.catalog.SupplyType;
import com.garageos.core.security.GarageScope;
import com.garageos.core.tax.GstCalculator;
import com.garageos.core.tax.TaxPolicy;
import com.garageos.core.util.RequestContext;
import com.garageos.modules.audit.service.AuditService;
import com.garageos.modules.catalog.dto.request.CorrectEstimateLineClassificationRequest;
import com.garageos.modules.catalog.dto.response.ReviewItemResponse;
import com.garageos.modules.catalog.entity.CatalogItem;
import com.garageos.modules.catalog.repository.GstTaxRuleRepository;
import com.garageos.modules.catalog.service.CatalogService;
import com.garageos.modules.catalog.service.EstimateLineClassificationService;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.estimate.repository.EstimateRepository;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.estimateitem.repository.EstimateItemRepository;
import com.garageos.modules.estimateitem.tax.EstimateTaxSupport;
import com.garageos.modules.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Financial safety design:
 * <ul>
 *   <li>Only the classification snapshot columns of the line (catalog id, HSN/SAC, tax rule, rate, review status)
 *       are written. Quantity, unit, price, line total, discount and every estimate field are never touched, and no
 *       recalculation routine is invoked.</li>
 *   <li>The effective rate the estimate was approved with (the line's own rate, otherwise the configured interim
 *       fallback) must equal the effective rate after the correction. If it would differ, or cannot be established,
 *       the correction is refused and a revised estimate with renewed customer approval is required.</li>
 *   <li>The estimate row is locked (SELECT ... FOR UPDATE) before invoice existence is checked; invoice issuance takes
 *       the same lock, so an invoice and a correction can never interleave on one estimate.</li>
 *   <li>The audit event is written in the same transaction; if it fails, the correction rolls back.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class EstimateLineClassificationServiceImpl implements EstimateLineClassificationService {

    private final EstimateItemRepository estimateItemRepository;
    private final EstimateRepository estimateRepository;
    private final InvoiceRepository invoiceRepository;
    private final CatalogService catalogService;
    private final GstTaxRuleRepository taxRuleRepository;
    private final AuditService auditService;
    private final TaxPolicy taxPolicy;
    private final GarageScope garageScope;

    @Override
    @Transactional
    public ReviewItemResponse correct(
            Long estimateId, Long estimateItemId, CorrectEstimateLineClassificationRequest request) {

        // Defence in depth behind the controller's SUPER_ADMIN rule.
        if (!garageScope.isUnrestricted(garageScope.principal())) {
            throw new ResourceNotFoundException("Not found.");
        }

        String reason = request.getReason() == null ? "" : request.getReason().trim();
        if (reason.length() < 10) {
            throw new BusinessException("A correction reason of at least 10 characters is required.");
        }

        // The line must belong to the estimate named in the path; checked without loading the line.
        Long owningEstimateId = estimateItemRepository.findEstimateIdById(estimateItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate item not found."));

        if (!Objects.equals(owningEstimateId, estimateId)) {
            throw new ResourceNotFoundException("Estimate item not found.");
        }

        // Lock first, then load the line and check the state the decision depends on, so neither is a pre-lock copy.
        Estimate estimate = estimateRepository.findByIdForUpdate(estimateId)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate not found."));

        EstimateItem line = estimateItemRepository.findById(estimateItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate item not found."));

        if (invoiceRepository.existsByEstimateId(estimateId)) {
            throw new ConflictException(
                    "This estimate already has an invoice and cannot be changed here. Correcting an issued invoice "
                            + "needs the accountant-approved credit-note/reissue process.",
                    "ESTIMATE_INVOICED");
        }

        if (estimate.getStatus() != EstimateStatus.APPROVED) {
            throw new ConflictException(
                    "Only an approved estimate is corrected with this action. For an estimate that is not approved, "
                            + "link the line from the review queue.",
                    "ESTIMATE_NOT_APPROVED");
        }

        CatalogItemType expected = expectedType(line);
        CatalogItem item = catalogService.requireUsable(request.getCatalogItemId(), expected);

        CatalogService.requireValidCode(expected, item.getHsnSacCode());

        if (item.getTaxRuleCode() != null && !taxRuleRepository.existsByRuleCode(item.getTaxRuleCode())) {
            throw new BusinessException("Unknown tax rule: " + item.getTaxRuleCode());
        }

        BigDecimal catalogRate = item.getClassificationStatus() == ClassificationStatus.VERIFIED
                ? catalogService.resolveRate(item.getTaxRuleCode(), LocalDate.now()).orElse(null)
                : null;

        // An unverified item has no rate of its own; it must not erase a rate already stored on the line.
        BigDecimal newRate = catalogRate != null ? catalogRate : line.getGstRatePercent();

        requireSameEffectiveRate(line, newRate);
        requireApprovedTotalsUnchanged(estimate, line, newRate);

        boolean unchanged = Objects.equals(line.getCatalogItemId(), item.getId())
                && Objects.equals(line.getHsnSacCode(), item.getHsnSacCode())
                && Objects.equals(line.getTaxRuleCode(), item.getTaxRuleCode())
                && sameRate(line.getGstRatePercent(), newRate);

        if (unchanged) {
            // Repeated request: nothing to write and no second audit event.
            return CatalogReviewServiceImpl.toResponse(line);
        }

        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("estimateId", estimateId);
        audit.put("estimateItemId", estimateItemId);
        audit.put("reason", reason);
        audit.put("catalogItemId", Map.of("before", String.valueOf(line.getCatalogItemId()), "after", String.valueOf(item.getId())));
        audit.put("hsnSacCode", Map.of("before", String.valueOf(line.getHsnSacCode()), "after", String.valueOf(item.getHsnSacCode())));
        audit.put("taxRuleCode", Map.of("before", String.valueOf(line.getTaxRuleCode()), "after", String.valueOf(item.getTaxRuleCode())));
        audit.put("gstRatePercent", Map.of("before", String.valueOf(line.getGstRatePercent()), "after", String.valueOf(newRate)));
        audit.put("catalogClassificationStatus", String.valueOf(item.getClassificationStatus()));
        audit.put("approvedTotalsReproducedBeforeAndAfter", true);
        audit.put("rateBasisBefore", line.getGstRatePercent() != null ? "STORED_LINE_RATE" : "APPROVED_TOTALS_REPRODUCED_UNDER_CURRENT_FALLBACK");
        audit.put("reviewStatusBefore", String.valueOf(line.getReviewStatus()));
        audit.put("requestId", String.valueOf(RequestContext.getRequestId()));

        line.setCatalogItemId(item.getId());
        line.setHsnSacCode(item.getHsnSacCode());
        line.setTaxRuleCode(item.getTaxRuleCode());
        line.setGstRatePercent(newRate);

        boolean classified = line.getHsnSacCode() != null && line.getGstRatePercent() != null;
        if (line.getReviewStatus() != ReviewStatus.NOT_REQUIRED) {
            line.setReviewStatus(classified ? ReviewStatus.RESOLVED : ReviewStatus.NEEDS_REVIEW);
        }
        audit.put("reviewStatusAfter", String.valueOf(line.getReviewStatus()));

        EstimateItem saved = estimateItemRepository.save(line);

        // Same transaction (AuditServiceImpl joins it): both persist or neither does.
        auditService.record(
                AuditEventType.ESTIMATE_LINE_CLASSIFICATION_CORRECTED,
                "ESTIMATE_ITEM",
                estimateItemId,
                line.getGarageId(),
                audit);

        return CatalogReviewServiceImpl.toResponse(saved == null ? line : saved);
    }

    /**
     * The rate the approved figures were built with must equal the rate after the correction. The line's own rate
     * wins; otherwise the configured interim fallback applies (exactly as {@code EstimateTaxSupport.resolve}).
     */
    private void requireSameEffectiveRate(EstimateItem line, BigDecimal newRate) {

        BigDecimal fallback = taxPolicy.fallbackRatePercent().orElse(null);

        BigDecimal before = line.getGstRatePercent() != null ? line.getGstRatePercent() : fallback;
        BigDecimal after = newRate != null ? newRate : fallback;

        if (before == null || after == null) {
            throw new ConflictException(
                    "The tax effect of this correction cannot be established because no GST rate is configured "
                            + "for this line. Revise the estimate and obtain the customer's approval again.",
                    "TAX_IMPACT_UNKNOWN");
        }

        if (before.compareTo(after) != 0) {
            throw new ConflictException(
                    "This classification would change the GST rate of the line from " + before.stripTrailingZeros().toPlainString()
                            + "% to " + after.stripTrailingZeros().toPlainString()
                            + "%, which changes what the customer approved. Create a revised estimate and obtain "
                            + "the customer's approval again.",
                    "CLASSIFICATION_CHANGES_TAX");
        }
    }

    /**
     * Financial equivalence, separate from classification validity. The estimate stores only its totals, not the
     * rate an unclassified line was approved with (that is resolved from the current interim fallback each time), so
     * equal "assumed" rates prove nothing. This check recomputes the totals from the stored lines through the same
     * canonical calculator (a) exactly as the lines are today and (b) with the proposed rate on the target line, and
     * requires BOTH to equal the stored approved subtotal, GST and grand total. If the stored figures cannot be
     * reproduced (a changed fallback, a legacy calculation, any drift) or the correction would move them, it fails
     * safely: a revised estimate and renewed customer approval are required.
     */
    private void requireApprovedTotalsUnchanged(Estimate estimate, EstimateItem line, BigDecimal newRate) {

        List<EstimateItem> selected = EstimateTaxSupport.selected(estimateItemRepository.findByEstimateId(estimate.getId()));

        GstCalculator.Result before;
        GstCalculator.Result after;

        try {
            before = totals(estimate, selected, line, line.getGstRatePercent(), false);
            after = totals(estimate, selected, line, newRate, true);
        } catch (BusinessException ex) {
            throw new ConflictException(
                    "The approved tax of this estimate cannot be established. Revise the estimate and obtain the "
                            + "customer's approval again.",
                    "TAX_IMPACT_UNKNOWN");
        }

        if (!matches(before, estimate)) {
            throw new ConflictException(
                    "The approved totals of this estimate cannot be reproduced from its lines (the interim rate may "
                            + "have changed since approval, or the estimate predates the current calculation), so a "
                            + "correction cannot be proven neutral. Revise the estimate and obtain the customer's "
                            + "approval again.",
                    "APPROVED_TOTALS_NOT_REPRODUCIBLE");
        }

        // Classification validity is not financial equivalence. A line with no stored rate was approved under an
        // ASSUMED (interim fallback) rate that is not persisted. For a tax-exclusive (labour) line the stored totals,
        // reproduced above, pin that rate. For a tax-inclusive (part) line the approved total does not depend on the
        // rate at all, so nothing persisted can establish it and an equal assumed rate proves nothing.
        if (line.getGstRatePercent() == null) {
            boolean pinnedByTotals = PriceTaxMode.forType(line.getItemType()) == PriceTaxMode.EXCLUSIVE
                    && line.getTotalPrice() != null && line.getTotalPrice().signum() > 0;
            if (!pinnedByTotals) {
                throw new ConflictException(
                        "The rate this line was approved with was never stored (an interim rate was assumed) and the "
                                + "approved totals do not depend on it, so the approved tax treatment cannot be "
                                + "established. Revise the estimate and obtain the customer's approval again.",
                        "APPROVED_RATE_NOT_ESTABLISHED");
            }
        }

        if (!matches(after, estimate)) {
            throw new ConflictException(
                    "This classification would change the approved subtotal, GST or grand total. Create a revised "
                            + "estimate and obtain the customer's approval again.",
                    "CLASSIFICATION_CHANGES_TAX");
        }
    }

    private GstCalculator.Result totals(
            Estimate estimate, List<EstimateItem> selected, EstimateItem target, BigDecimal targetRate, boolean replace) {

        List<GstCalculator.LineInput> inputs = new java.util.ArrayList<>(selected.size());

        for (EstimateItem item : selected) {
            EstimateItem effective = item;
            if (replace && Objects.equals(item.getId(), target.getId())) {
                // A detached copy: only the rate differs; nothing is persisted.
                effective = new EstimateItem();
                effective.setId(item.getId());
                effective.setItemType(item.getItemType());
                effective.setDescription(item.getDescription());
                effective.setQuantity(item.getQuantity());
                effective.setUnitPrice(item.getUnitPrice());
                effective.setTotalPrice(item.getTotalPrice());
                effective.setHsnSacCode(item.getHsnSacCode());
                effective.setGstRatePercent(targetRate);
            }
            inputs.add(EstimateTaxSupport.resolve(effective, taxPolicy).input());
        }

        return GstCalculator.calculate(inputs, estimate.getDiscount(), SupplyType.INTRA_STATE);
    }

    private static boolean matches(GstCalculator.Result result, Estimate estimate) {
        return estimate.getSubtotal() != null && estimate.getGst() != null && estimate.getGrandTotal() != null
                && result.subtotal().compareTo(estimate.getSubtotal()) == 0
                && result.addedGst().compareTo(estimate.getGst()) == 0
                && result.grandTotal().compareTo(estimate.getGrandTotal()) == 0;
    }

    private static boolean sameRate(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
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
}
