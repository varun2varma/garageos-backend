package com.garageos.core.util;

import com.garageos.core.enums.EstimateItemType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Invoice tax decomposition over the FINAL billable line items.
 *
 * Business rules (all arithmetic is {@link BigDecimal}, scale 2, HALF_UP):
 * <ul>
 *   <li><b>PART</b> prices are entered GST-<b>inclusive</b>. The line total
 *       is exactly what was entered; taxable value = total / 1.18 and GST =
 *       total - taxable (reverse calculation). Never total x 18%.</li>
 *   <li><b>LABOUR</b> prices are GST-<b>exclusive</b>: GST is added on top
 *       (taxable = entered price, total = taxable + 18%).</li>
 *   <li><b>OTHERS</b> (consumables) carry no GST - same rule as
 *       {@link MoneyCalculator#calculateGST}.</li>
 *   <li>A discount reduces the labour taxable base exactly the way
 *       {@link MoneyCalculator#calculateGST}/{@code calculateGrandTotal} do
 *       for the estimate, so the invoice grand total equals the stored
 *       estimate/invoice grand total before the rupee round-off.</li>
 *   <li>CGST/SGST split the total GST evenly (intra-state); any odd paisa
 *       goes to SGST so CGST + SGST always equals the GST exactly.</li>
 * </ul>
 *
 * Stateless and side-effect free so it can be unit-tested exhaustively and
 * reused anywhere an invoice is rendered.
 */
public final class InvoiceTaxCalculator {

    /** Default GST rate (percent) for motor-vehicle parts and repair services. */
    public static final BigDecimal GST_RATE_PERCENT = new BigDecimal("18");

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final BigDecimal TWO = new BigDecimal("2");

    private InvoiceTaxCalculator() {
    }

    /** One billable line: type, description, quantity, unit price and stored line total. */
    public record Line(
            EstimateItemType type,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice) {
    }

    public record LineResult(
            EstimateItemType type,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal taxableValue,
            BigDecimal gst,
            BigDecimal total) {
    }

    public record Breakdown(
            List<LineResult> parts,
            List<LineResult> labour,
            List<LineResult> others,
            BigDecimal partsTotal,
            BigDecimal partsTaxable,
            BigDecimal partsGst,
            BigDecimal labourSubtotal,
            BigDecimal discount,
            BigDecimal labourTaxable,
            BigDecimal labourGst,
            BigDecimal labourTotal,
            BigDecimal othersTotal,
            BigDecimal totalGst,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal exactTotal,
            BigDecimal roundOff,
            BigDecimal grandTotal,
            BigDecimal gstRatePercent) {
    }

    public static Breakdown calculate(List<Line> lines, BigDecimal discount) {
        return calculate(lines, discount, GST_RATE_PERCENT);
    }

    /** Rate-driven: every GST figure derives from {@code gstRatePercent} (e.g. 18). */
    public static Breakdown calculate(List<Line> lines, BigDecimal discount, BigDecimal gstRatePercent) {

        BigDecimal discountValue = scale(discount == null ? BigDecimal.ZERO : discount);

        List<LineResult> parts = new ArrayList<>();
        List<LineResult> labour = new ArrayList<>();
        List<LineResult> others = new ArrayList<>();

        for (Line line : lines) {

            BigDecimal total = lineTotal(line);

            switch (line.type()) {
                case PART -> parts.add(partLine(line, total, gstRatePercent));
                case LABOUR -> labour.add(labourLine(line, total, gstRatePercent));
                default -> others.add(new LineResult(
                        line.type(), line.description(), line.quantity(), line.unitPrice(),
                        total, BigDecimal.ZERO.setScale(2), total));
            }
        }

        BigDecimal partsTotal = sum(parts, LineResult::total);
        BigDecimal partsTaxable = sum(parts, LineResult::taxableValue);
        BigDecimal partsGst = partsTotal.subtract(partsTaxable);

        BigDecimal labourSubtotal = sum(labour, LineResult::taxableValue);

        // Same discount-on-labour-base rule as MoneyCalculator.calculateGST,
        // GST never negative.
        BigDecimal labourTaxable = labourSubtotal.subtract(discountValue);
        if (labourTaxable.signum() < 0) {
            labourTaxable = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal labourGst = labourTaxable
                .multiply(gstRatePercent)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        BigDecimal labourTotal = labourTaxable.add(labourGst);

        BigDecimal othersTotal = sum(others, LineResult::total);

        BigDecimal totalGst = partsGst.add(labourGst);
        BigDecimal cgst = totalGst.divide(TWO, 2, RoundingMode.DOWN);
        BigDecimal sgst = totalGst.subtract(cgst);

        // Equals estimate/invoice grandTotal: subtotal - discount + gst(labour).
        BigDecimal exactTotal = partsTotal
                .add(othersTotal)
                .add(labourSubtotal)
                .subtract(discountValue)
                .add(labourGst);

        BigDecimal grandTotal = exactTotal.setScale(0, RoundingMode.HALF_UP).setScale(2);
        BigDecimal roundOff = grandTotal.subtract(exactTotal);

        return new Breakdown(
                parts, labour, others,
                partsTotal, partsTaxable, partsGst,
                labourSubtotal, discountValue, labourTaxable, labourGst, labourTotal,
                othersTotal,
                totalGst, cgst, sgst,
                exactTotal, roundOff, grandTotal, gstRatePercent);
    }

    private static LineResult partLine(Line line, BigDecimal total, BigDecimal ratePercent) {

        // GST-inclusive: reverse-calculate. taxable = total / (1 + rate/100).
        BigDecimal divisor = BigDecimal.ONE.add(ratePercent.divide(HUNDRED));
        BigDecimal taxable = total.divide(divisor, 2, RoundingMode.HALF_UP);
        BigDecimal gst = total.subtract(taxable);

        return new LineResult(
                line.type(), line.description(), line.quantity(), line.unitPrice(),
                taxable, gst, total);
    }

    private static LineResult labourLine(Line line, BigDecimal total, BigDecimal ratePercent) {

        // GST-exclusive: add GST on top of the entered price.
        BigDecimal gst = total
                .multiply(ratePercent)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        return new LineResult(
                line.type(), line.description(), line.quantity(), line.unitPrice(),
                total, gst, total.add(gst));
    }

    private static BigDecimal lineTotal(Line line) {

        if (line.totalPrice() != null) {
            return scale(line.totalPrice());
        }

        return MoneyCalculator.calculateItemTotal(line.quantity(), line.unitPrice());
    }

    private static BigDecimal sum(
            List<LineResult> lines,
            java.util.function.Function<LineResult, BigDecimal> field) {

        return lines.stream()
                .map(field)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
