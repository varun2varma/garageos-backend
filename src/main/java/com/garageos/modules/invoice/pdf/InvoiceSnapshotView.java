package com.garageos.modules.invoice.pdf;

import com.garageos.core.enums.EstimateItemType;
import com.garageos.core.enums.catalog.SupplyType;
import com.garageos.core.util.InvoiceTaxCalculator.Breakdown;
import com.garageos.core.util.InvoiceTaxCalculator.LineResult;
import com.garageos.modules.invoice.entity.Invoice;
import com.garageos.modules.invoiceitem.entity.InvoiceItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Read model of a NEW invoice, built only from the figures persisted when it was
 * issued (invoice header and invoice lines). It performs no tax arithmetic, so a
 * later catalog, rule or rate change cannot alter what is printed or reported.
 */
public record InvoiceSnapshotView(
        List<Line> lines,
        SupplyType supplyType,
        String placeOfSupplyState,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal taxableTotal,
        BigDecimal cgstTotal,
        BigDecimal sgstTotal,
        BigDecimal igstTotal,
        BigDecimal taxTotal,
        BigDecimal roundOff,
        BigDecimal grandTotal) {

    /** One printed line; {@code hsnSacCode} stays null when the line is unclassified. */
    public record Line(
            EstimateItemType type,
            String description,
            String hsnSacCode,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal ratePercent,
            BigDecimal taxableValue,
            BigDecimal taxAmount,
            BigDecimal lineTotal) {
    }

    public static InvoiceSnapshotView of(Invoice invoice, List<InvoiceItem> items) {

        List<Line> lines = new ArrayList<>(items.size());
        BigDecimal taxable = BigDecimal.ZERO;

        for (InvoiceItem item : items) {
            lines.add(new Line(
                    item.getItemType(), item.getDescription(), item.getHsnSacCode(),
                    item.getQuantity(), item.getUnit(), item.getUnitPrice(),
                    item.getGstRatePercent(), item.getTaxableValue(), item.getTaxAmount(),
                    item.getLineTotal()));
            taxable = taxable.add(zero(item.getTaxableValue()));
        }

        return new InvoiceSnapshotView(
                lines, invoice.getSupplyType(), invoice.getPlaceOfSupplyState(),
                invoice.getSubtotal(), invoice.getDiscount(), taxable,
                zero(invoice.getCgstTotal()), zero(invoice.getSgstTotal()), zero(invoice.getIgstTotal()),
                zero(invoice.getTaxTotal()), zero(invoice.getRoundOff()), invoice.getGrandTotal());
    }

    /**
     * The persisted figures in the shape the final-report and e-mail code already
     * consume. Amounts are copied, never recomputed; the grand total is the invoice's
     * own persisted total, so reports, PDF and payment validation agree.
     */
    public static Breakdown breakdownOf(Invoice invoice, List<InvoiceItem> items) {

        List<LineResult> parts = new ArrayList<>();
        List<LineResult> labour = new ArrayList<>();
        List<LineResult> others = new ArrayList<>();

        for (InvoiceItem item : items) {
            LineResult result = new LineResult(
                    item.getItemType(), item.getDescription(), item.getQuantity(), item.getUnitPrice(),
                    zero(item.getTaxableValue()), zero(item.getTaxAmount()), zero(item.getLineTotal()));
            switch (item.getItemType()) {
                case PART -> parts.add(result);
                case LABOUR -> labour.add(result);
                default -> others.add(result);
            }
        }

        BigDecimal grandTotal = invoice.getGrandTotal();
        BigDecimal roundOff = zero(invoice.getRoundOff());

        return new Breakdown(
                parts, labour, others,
                sum(parts, LineResult::total), sum(parts, LineResult::taxableValue), sum(parts, LineResult::gst),
                sumEntered(items, EstimateItemType.LABOUR), zero(invoice.getDiscount()),
                sum(labour, LineResult::taxableValue), sum(labour, LineResult::gst), sum(labour, LineResult::total),
                sum(others, LineResult::total),
                zero(invoice.getTaxTotal()), zero(invoice.getCgstTotal()), zero(invoice.getSgstTotal()),
                grandTotal.subtract(roundOff), roundOff, grandTotal,
                null);
    }

    private static BigDecimal sum(List<LineResult> lines, Function<LineResult, BigDecimal> field) {
        return lines.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumEntered(List<InvoiceItem> items, EstimateItemType type) {
        return items.stream()
                .filter(i -> i.getItemType() == type)
                .map(InvoiceItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
