package com.garageos.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyCalculator {

    private static final BigDecimal GST_PERCENTAGE =
            new BigDecimal("18");

    private static final BigDecimal HUNDRED =
            new BigDecimal("100");

    private MoneyCalculator() {
    }

    public static BigDecimal calculateItemTotal(
            BigDecimal quantity,
            BigDecimal unitPrice) {

        return quantity.multiply(unitPrice)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Mission's own explicit tax rule: GST applies to LABOUR charges
     * only - never to PARTS, never to OTHERS/consumables. {@code
     * labourSubtotal} must therefore already be the LABOUR-only portion
     * of the estimate (see EstimateItemServiceImpl.recalculateEstimate),
     * not the whole estimate subtotal - this method has no way to enforce
     * that itself, since it only ever sees a number.
     */
    public static BigDecimal calculateGST(
            BigDecimal labourSubtotal,
            BigDecimal discount) {

        BigDecimal taxableAmount =
                labourSubtotal.subtract(discount);

        // A discount can legitimately exceed the labour-only portion of
        // an estimate (e.g. a flat-amount discount on a parts-heavy job) -
        // GST must never go negative in that case.
        if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) {
            taxableAmount = BigDecimal.ZERO;
        }

        return taxableAmount
                .multiply(GST_PERCENTAGE)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateGrandTotal(
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal gst) {

        return subtotal
                .subtract(discount)
                .add(gst)
                .setScale(2, RoundingMode.HALF_UP);
    }

}