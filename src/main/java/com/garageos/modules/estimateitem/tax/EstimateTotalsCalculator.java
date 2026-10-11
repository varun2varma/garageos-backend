package com.garageos.modules.estimateitem.tax;

import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.catalog.SupplyType;
import com.garageos.core.tax.GstCalculator;
import com.garageos.core.tax.TaxPolicy;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.estimate.repository.EstimateRepository;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.estimateitem.repository.EstimateItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Recomputes and persists an estimate's totals through the canonical
 * {@link GstCalculator}. {@code estimate.gst} keeps its established meaning
 * (GST added on top of tax-exclusive lines), so {@code subtotal - discount +
 * gst} still explains the grand total apart from the whole-rupee round-off,
 * which is included in {@code grandTotal} exactly as it will be on the invoice.
 */
@Component
@RequiredArgsConstructor
public class EstimateTotalsCalculator {

    private final EstimateItemRepository itemRepository;
    private final EstimateRepository estimateRepository;
    private final TaxPolicy taxPolicy;

    public void recalculate(Estimate estimate) {

        List<EstimateItem> items = itemRepository.findByEstimateId(estimate.getId());

        // A deselected item must not continue to repair/invoice, so it is excluded
        // here, at the one place estimate totals are computed.
        List<EstimateItem> selected = EstimateTaxSupport.selected(items);

        // The CGST/SGST versus IGST split does not change any total; it is decided
        // from the parties when the invoice is issued.
        GstCalculator.Result result = GstCalculator.calculate(
                EstimateTaxSupport.inputs(EstimateTaxSupport.resolveAll(selected, taxPolicy)),
                estimate.getDiscount(),
                SupplyType.INTRA_STATE);

        estimate.setSubtotal(result.subtotal());
        estimate.setGst(result.addedGst());
        estimate.setGrandTotal(result.grandTotal());
        estimate.setStatus(EstimateStatus.WAITING_FOR_APPROVAL);

        estimateRepository.save(estimate);
    }
}
