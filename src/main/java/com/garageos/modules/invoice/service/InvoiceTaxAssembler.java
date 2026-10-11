package com.garageos.modules.invoice.service;

import com.garageos.core.enums.catalog.SupplyType;
import com.garageos.core.enums.catalog.TaxSource;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.tax.GstCalculator;
import com.garageos.core.tax.TaxPolicy;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.estimateitem.tax.EstimateTaxSupport;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.invoice.entity.Invoice;
import com.garageos.modules.invoiceitem.entity.InvoiceItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Builds the immutable tax snapshot of a NEW invoice from the approved
 * estimate's selected lines, through the canonical {@link GstCalculator}.
 * The invoice header and every invoice line are filled from one calculation, so
 * the API, PDF, reports and payment validation all read identical figures.
 *
 * Nothing here recomputes an existing invoice; legacy invoices keep the values
 * they were issued with.
 */
public final class InvoiceTaxAssembler {

    private InvoiceTaxAssembler() {
    }

    /**
     * Sets the header totals on {@code invoice} and returns its (unsaved) lines.
     *
     * @param garage   supplier; may be null only for malformed legacy data
     * @param customer recipient; may be null
     */
    public static List<InvoiceItem> assemble(
            Invoice invoice,
            Estimate estimate,
            List<EstimateItem> allItems,
            Garage garage,
            Customer customer,
            TaxPolicy policy) {

        List<EstimateItem> selected = EstimateTaxSupport.selected(allItems);

        if (policy.requireClassification()) {
            String unclassified = selected.stream()
                    .filter(EstimateTaxSupport::unclassified)
                    .map(EstimateItem::getDescription)
                    .collect(Collectors.joining(", "));
            if (!unclassified.isEmpty()) {
                throw new BusinessException(
                        "Invoice cannot be issued until these items are classified: " + unclassified);
            }
        }

        String supplierState = garage == null ? null : blankToNull(garage.getState());
        String recipientState = customer == null ? null : blankToNull(customer.getState());

        SupplyType supply = supplyType(supplierState, recipientState);

        List<EstimateTaxSupport.Resolved> resolved = EstimateTaxSupport.resolveAll(selected, policy);

        GstCalculator.Result result = GstCalculator.calculate(
                EstimateTaxSupport.inputs(resolved), estimate.getDiscount(), supply);

        invoice.setTaxSnapshot(true);
        invoice.setSubtotal(result.subtotal());
        invoice.setDiscount(result.discount());
        invoice.setGst(result.addedGst());
        invoice.setTaxTotal(result.taxTotal());
        invoice.setCgstTotal(result.cgstTotal());
        invoice.setSgstTotal(result.sgstTotal());
        invoice.setIgstTotal(result.igstTotal());
        invoice.setRoundOff(result.roundOff());
        invoice.setGrandTotal(result.grandTotal());
        invoice.setSupplyType(supply);
        invoice.setSupplierGstin(garage == null ? null : blankToNull(garage.getGstNumber()));
        invoice.setSupplierState(supplierState);
        invoice.setPlaceOfSupplyState(recipientState != null ? recipientState : supplierState);

        List<InvoiceItem> lines = new ArrayList<>(selected.size());

        for (int i = 0; i < selected.size(); i++) {

            EstimateItem source = selected.get(i);
            GstCalculator.LineResult calc = result.lines().get(i);
            TaxSource taxSource = resolved.get(i).source();

            InvoiceItem line = new InvoiceItem();
            line.setInvoice(invoice);
            line.setComplaint(source.getComplaint());
            line.setItemType(source.getItemType());
            line.setDescription(source.getDescription());
            line.setQuantity(source.getQuantity());
            line.setUnitPrice(source.getUnitPrice());
            line.setTotalPrice(calc.enteredTotal());

            line.setCatalogItemId(source.getCatalogItemId());
            line.setUnit(source.getUnit());
            line.setHsnSacCode(source.getHsnSacCode());
            line.setTaxRuleCode(source.getTaxRuleCode());
            line.setPriceTaxMode(calc.mode());
            line.setTaxSource(taxSource);
            line.setGstRatePercent(calc.ratePercent());
            line.setTaxableValue(calc.taxableValue());
            line.setCgstAmount(calc.cgst());
            line.setSgstAmount(calc.sgst());
            line.setIgstAmount(calc.igst());
            line.setTaxAmount(calc.taxAmount());
            line.setLineTotal(calc.lineTotal());

            lines.add(line);
        }

        return lines;
    }

    /**
     * Inter-state only when both states are known and differ. When the recipient
     * state is unknown the supply is treated as taking place at the supplier's
     * location (intra-state); the place of supply recorded on the invoice makes
     * that visible for accountant review.
     */
    public static SupplyType supplyType(String supplierState, String recipientState) {

        if (supplierState == null || recipientState == null) {
            return SupplyType.INTRA_STATE;
        }

        return normalizeState(supplierState).equals(normalizeState(recipientState))
                ? SupplyType.INTRA_STATE
                : SupplyType.INTER_STATE;
    }

    private static String normalizeState(String state) {
        return state.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
