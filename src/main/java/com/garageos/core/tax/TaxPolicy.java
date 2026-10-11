package com.garageos.core.tax;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Deployment-level GST policy for lines that carry no classified tax rule.
 *
 * <ul>
 *   <li>{@code garagest.tax.fallback-rate-percent}: interim rate applied to an
 *       unclassified PART/LABOUR line so existing garages keep invoicing before
 *       the catalog is populated. Blank/negative disables the fallback. The line
 *       is flagged {@code CONFIGURED_FALLBACK}; it is NOT a verified rate.</li>
 *   <li>{@code garagest.tax.require-classification}: when true, an invoice cannot
 *       be issued while a taxable line lacks a verified classification.</li>
 * </ul>
 */
@Component
public class TaxPolicy {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TaxPolicy.class);

    private final BigDecimal fallbackRatePercent;

    private final boolean requireClassification;

    @Autowired
    public TaxPolicy(
            @Value("${garagest.tax.fallback-rate-percent:}") String fallbackRatePercent,
            @Value("${garagest.tax.require-classification:false}") boolean requireClassification) {
        this.fallbackRatePercent = parse(fallbackRatePercent);
        this.requireClassification = requireClassification;
        logEffectivePolicy();
    }

    private TaxPolicy(BigDecimal fallbackRatePercent, boolean requireClassification) {
        this.fallbackRatePercent = fallbackRatePercent;
        this.requireClassification = requireClassification;
    }

    /** Policy used when no Spring context is present (unit tests): 18% interim fallback, no gate. */
    public static TaxPolicy legacyCompatible() {
        return new TaxPolicy(new BigDecimal("18"), false);
    }

    public static TaxPolicy of(BigDecimal fallbackRatePercent, boolean requireClassification) {
        return new TaxPolicy(fallbackRatePercent, requireClassification);
    }

    /** States the effective policy in the startup log so an operator can see how unclassified lines are treated. */
    private void logEffectivePolicy() {
        if (requireClassification) {
            LOG.warn("[GST] require-classification=true: an invoice cannot be issued while a taxable line lacks a verified HSN/SAC and rate.");
        } else if (fallbackRatePercent != null) {
            LOG.warn("[GST] Unclassified taxable lines are billed at the INTERIM configured rate of {}%. "
                    + "This is not a verified rate; set garagest.tax.require-classification=true once the catalog is verified.", fallbackRatePercent);
        } else {
            LOG.warn("[GST] No interim rate is configured: taxable lines without a verified rate cannot be added.");
        }
    }

    public Optional<BigDecimal> fallbackRatePercent() {
        return Optional.ofNullable(fallbackRatePercent);
    }

    public boolean requireClassification() {
        return requireClassification;
    }

    private static BigDecimal parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(raw.trim());
            return value.signum() < 0 || value.compareTo(new BigDecimal("100")) > 0 ? null : value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
