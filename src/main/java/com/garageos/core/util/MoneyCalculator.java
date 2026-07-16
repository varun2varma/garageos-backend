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

    public static BigDecimal calculateGST(
            BigDecimal subtotal,
            BigDecimal discount) {

        BigDecimal taxableAmount =
                subtotal.subtract(discount);

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