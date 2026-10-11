package com.garageos.modules.estimateitem.tax;

import com.garageos.core.enums.catalog.PriceTaxMode;
import com.garageos.core.enums.catalog.TaxSource;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.tax.GstCalculator;
import com.garageos.core.tax.TaxPolicy;
import com.garageos.modules.estimateitem.entity.EstimateItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns estimate lines into canonical calculator inputs. Shared by estimate
 * recalculation and invoice issuance so both read a line's rate the same way:
 * the line's own classification snapshot first, then the configured interim
 * fallback, never a number baked into code.
 */
public final class EstimateTaxSupport {

    private EstimateTaxSupport() {
    }

    /** The lines the customer kept, in stable order. */
    public static List<EstimateItem> selected(List<EstimateItem> items) {
        return items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getSelected()))
                .toList();
    }

    public record Resolved(GstCalculator.LineInput input, TaxSource source) {
    }

    public static Resolved resolve(EstimateItem item, TaxPolicy policy) {

        PriceTaxMode mode = PriceTaxMode.forType(item.getItemType());

        if (mode == PriceTaxMode.NO_TAX) {
            return new Resolved(input(item, null), TaxSource.NONE);
        }

        if (item.getGstRatePercent() != null) {
            return new Resolved(input(item, item.getGstRatePercent()), TaxSource.CATALOG_RULE);
        }

        BigDecimal fallback = policy.fallbackRatePercent().orElseThrow(() -> new BusinessException(
                "No GST rate is configured for \"" + item.getDescription()
                        + "\". Pick a classified catalog item or ask an administrator to configure the tax rate."));

        return new Resolved(input(item, fallback), TaxSource.CONFIGURED_FALLBACK);
    }

    public static List<Resolved> resolveAll(List<EstimateItem> items, TaxPolicy policy) {
        List<Resolved> resolved = new ArrayList<>(items.size());
        for (EstimateItem item : items) {
            resolved.add(resolve(item, policy));
        }
        return resolved;
    }

    public static List<GstCalculator.LineInput> inputs(List<Resolved> resolved) {
        return resolved.stream().map(Resolved::input).toList();
    }

    /** True for a taxable line that has no verified classification snapshot. */
    public static boolean unclassified(EstimateItem item) {
        return PriceTaxMode.forType(item.getItemType()) != PriceTaxMode.NO_TAX
                && (item.getHsnSacCode() == null || item.getGstRatePercent() == null);
    }

    private static GstCalculator.LineInput input(EstimateItem item, BigDecimal rate) {
        return new GstCalculator.LineInput(
                item.getItemType(), item.getQuantity(), item.getUnitPrice(), item.getTotalPrice(), rate);
    }
}
