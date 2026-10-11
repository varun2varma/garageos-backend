package com.garageos.core.tax;

import com.garageos.core.enums.EstimateItemType;
import com.garageos.core.enums.catalog.PriceTaxMode;
import com.garageos.core.enums.catalog.SupplyType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * The single GST calculation for NEW estimates and invoices.
 *
 * Pure and stateless: every rate arrives with the line (resolved from the
 * line's classification snapshot or the configured fallback by the caller),
 * so no percentage is hardcoded here. All arithmetic is BigDecimal, scale 2,
 * HALF_UP; the payable total is rounded to the whole rupee and the round-off
 * is reported separately so every consumer (API, PDF, reports, payment
 * validation) reads the same persisted number.
 *
 * Price semantics are unchanged from the legacy calculators and are decided
 * by {@link PriceTaxMode#forType}: PART prices are GST-inclusive, LABOUR
 * (service) prices are GST-exclusive, OTHERS carry no GST.
 *
 * A discount reduces the taxable value of the tax-EXCLUSIVE lines (labour),
 * allocated proportionally; any discount beyond those lines reduces the total
 * but never creates negative tax, exactly like the legacy rule.
 *
 * Tax is split per line: CGST + SGST (intra-state, odd paisa to SGST) or IGST
 * (inter-state), so line figures always add up to the invoice figures.
 */
public final class GstCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final BigDecimal TWO = new BigDecimal("2");

    private GstCalculator() {
    }

    /** {@code totalPrice} may be null, in which case quantity x unitPrice is used. {@code ratePercent} may be null only for NO_TAX lines. */
    public record LineInput(
            EstimateItemType type,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            BigDecimal ratePercent) {
    }

    public record LineResult(
            int index,
            PriceTaxMode mode,
            BigDecimal ratePercent,
            BigDecimal enteredTotal,
            BigDecimal discountAllocated,
            BigDecimal taxableValue,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal igst,
            BigDecimal taxAmount,
            BigDecimal lineTotal) {
    }

    public record Result(
            List<LineResult> lines,
            SupplyType supplyType,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal unallocatedDiscount,
            BigDecimal taxableTotal,
            /** GST added on top of tax-exclusive lines (the legacy meaning of invoice.gst). */
            BigDecimal addedGst,
            /** Total GST: added GST plus the GST contained in tax-inclusive prices. */
            BigDecimal taxTotal,
            BigDecimal cgstTotal,
            BigDecimal sgstTotal,
            BigDecimal igstTotal,
            BigDecimal exactTotal,
            BigDecimal roundOff,
            BigDecimal grandTotal) {
    }

    public static Result calculate(List<LineInput> inputs, BigDecimal discount, SupplyType supplyType) {

        SupplyType supply = supplyType == null ? SupplyType.INTRA_STATE : supplyType;

        BigDecimal discountValue = scale(discount == null ? BigDecimal.ZERO : discount.max(BigDecimal.ZERO));

        int size = inputs.size();
        PriceTaxMode[] modes = new PriceTaxMode[size];
        BigDecimal[] entered = new BigDecimal[size];

        BigDecimal exclusiveSum = BigDecimal.ZERO;
        int lastExclusive = -1;

        for (int i = 0; i < size; i++) {
            LineInput in = inputs.get(i);
            modes[i] = PriceTaxMode.forType(in.type());
            entered[i] = enteredTotal(in);
            if (modes[i] == PriceTaxMode.EXCLUSIVE) {
                exclusiveSum = exclusiveSum.add(entered[i]);
                lastExclusive = i;
            }
        }

        BigDecimal allocatable = discountValue.min(exclusiveSum);
        BigDecimal unallocated = discountValue.subtract(allocatable);

        List<LineResult> lines = new ArrayList<>(size);
        BigDecimal allocatedSoFar = BigDecimal.ZERO;

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal addedGst = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        BigDecimal cgstTotal = BigDecimal.ZERO;
        BigDecimal sgstTotal = BigDecimal.ZERO;
        BigDecimal igstTotal = BigDecimal.ZERO;
        BigDecimal linesTotal = BigDecimal.ZERO;

        for (int i = 0; i < size; i++) {

            LineInput in = inputs.get(i);
            BigDecimal total = entered[i];
            BigDecimal alloc = BigDecimal.ZERO.setScale(2);
            BigDecimal lineTaxable;
            BigDecimal tax;
            BigDecimal lineTotal;

            switch (modes[i]) {
                case EXCLUSIVE -> {
                    requireRate(in);
                    if (allocatable.signum() > 0 && exclusiveSum.signum() > 0) {
                        alloc = i == lastExclusive
                                ? allocatable.subtract(allocatedSoFar)
                                : allocatable.multiply(total).divide(exclusiveSum, 2, RoundingMode.HALF_UP);
                        allocatedSoFar = allocatedSoFar.add(alloc);
                    }
                    lineTaxable = total.subtract(alloc);
                    tax = percentOf(lineTaxable, in.ratePercent());
                    lineTotal = lineTaxable.add(tax);
                    addedGst = addedGst.add(tax);
                }
                case INCLUSIVE -> {
                    requireRate(in);
                    BigDecimal divisor = BigDecimal.ONE.add(in.ratePercent().divide(HUNDRED));
                    lineTaxable = total.divide(divisor, 2, RoundingMode.HALF_UP);
                    tax = total.subtract(lineTaxable);
                    lineTotal = total;
                }
                default -> {
                    lineTaxable = total;
                    tax = BigDecimal.ZERO.setScale(2);
                    lineTotal = total;
                }
            }

            BigDecimal cgst = BigDecimal.ZERO.setScale(2);
            BigDecimal sgst = BigDecimal.ZERO.setScale(2);
            BigDecimal igst = BigDecimal.ZERO.setScale(2);

            if (tax.signum() != 0) {
                if (supply == SupplyType.INTER_STATE) {
                    igst = tax;
                } else {
                    cgst = tax.divide(TWO, 2, RoundingMode.DOWN);
                    sgst = tax.subtract(cgst);
                }
            }

            lines.add(new LineResult(
                    i, modes[i],
                    modes[i] == PriceTaxMode.NO_TAX ? null : in.ratePercent(),
                    total, alloc, lineTaxable, cgst, sgst, igst, tax, lineTotal));

            subtotal = subtotal.add(total);
            taxable = taxable.add(lineTaxable);
            taxTotal = taxTotal.add(tax);
            cgstTotal = cgstTotal.add(cgst);
            sgstTotal = sgstTotal.add(sgst);
            igstTotal = igstTotal.add(igst);
            linesTotal = linesTotal.add(lineTotal);
        }

        BigDecimal exactTotal = linesTotal.subtract(unallocated);
        BigDecimal grandTotal = exactTotal.setScale(0, RoundingMode.HALF_UP).setScale(2);
        BigDecimal roundOff = grandTotal.subtract(exactTotal);

        return new Result(
                lines, supply, subtotal, discountValue, unallocated, taxable,
                addedGst, taxTotal, cgstTotal, sgstTotal, igstTotal,
                exactTotal, roundOff, grandTotal);
    }

    private static void requireRate(LineInput in) {
        if (in.ratePercent() == null || in.ratePercent().signum() < 0) {
            throw new IllegalArgumentException(
                    "A GST rate is required for taxable line: " + in.type());
        }
    }

    private static BigDecimal percentOf(BigDecimal base, BigDecimal ratePercent) {
        return base.multiply(ratePercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal enteredTotal(LineInput in) {
        if (in.totalPrice() != null) {
            return scale(in.totalPrice());
        }
        return scale(in.quantity().multiply(in.unitPrice()));
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
