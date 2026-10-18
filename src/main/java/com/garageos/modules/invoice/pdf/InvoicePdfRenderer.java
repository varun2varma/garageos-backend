package com.garageos.modules.invoice.pdf;

import com.garageos.core.enums.catalog.SupplyType;
import com.garageos.core.pdf.GarageStPageFooter;
import com.garageos.core.util.AmountInWords;
import com.garageos.core.util.InvoiceTaxCalculator.Breakdown;
import com.garageos.core.util.InvoiceTaxCalculator.LineResult;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.garageos.core.pdf.GarageStPdf.*;

/**
 * Renders the GarageST TAX INVOICE PDF from {@link InvoiceDocumentData}.
 *
 * Layout (top to bottom): garage logo + name/address/contact/GSTIN with a
 * "Powered by GarageST" line, TAX INVOICE title, invoice/job-card/vehicle/
 * advisor meta, customer + vehicle blocks, PARTS table, LABOUR / SERVICES
 * table, optional OTHER CHARGES, tax summary (CGST/SGST/round-off/grand
 * total), amount in words, payment status (only when the invoice carries
 * one), and signature areas. The "computer-generated" note and page numbers
 * live in the page footer, not the body, so they can never create an empty
 * trailing page. Nothing garage-specific is hardcoded.
 *
 * Presentation only: every amount comes from the already-computed
 * {@code Breakdown}; this class performs no tax arithmetic.
 *
 * Shared styling (palette, Noto Sans, header, footer) lives in
 * {@link com.garageos.core.pdf.GarageStPdf}, which the final Job Card
 * reports use too.
 */
@Component
public class InvoicePdfRenderer {

    /** Shown where a line has no saved classification; a code is never invented. */
    static final String NO_CODE = "-";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public byte[] render(InvoiceDocumentData data) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4, 32, 32, 28, GarageStPageFooter.BOTTOM_MARGIN);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new GarageStPageFooter(
                    "This is a computer-generated tax invoice.", data.invoiceNumber()));

            applyMetadata(document, "Tax Invoice " + data.invoiceNumber(),
                    "Job Card " + data.jobCardNumber());

            document.open();

            document.add(garageHeader(data.garage(), data.logoBytes()));
            document.add(titleBar("TAX INVOICE"));
            document.add(metaBlock(data));
            document.add(partiesBlock(data));

            Breakdown b = data.breakdown();

            InvoiceSnapshotView snap = data.snapshot();

            if (snap != null) {
                document.add(supplyBlock(snap));
                document.add(snapshotLines(snap));
            } else {
                document.add(lineSection(
                        "PARTS (prices are GST-inclusive)",
                        "Part", "HSN", NO_CODE, b.parts()));

                document.add(lineSection(
                        "LABOUR / SERVICES (GST added on price)",
                        "Service", "SAC", NO_CODE, b.labour()));

                if (!b.others().isEmpty()) {
                    document.add(othersSection(b.others()));
                }
            }

            // Totals and signatures stay together: when a long invoice overflows, they move
            // to the next page as one block instead of stranding a lone signature line.
            PdfPTable closing = new PdfPTable(1);
            closing.setWidthPercentage(100);
            closing.setKeepTogether(true);
            PdfPCell closingCell = noBorder();
            closingCell.addElement(snap != null ? snapshotSummary(snap, data) : summaryBlock(b, data));
            closingCell.addElement(signatureBlock(data));
            closing.addCell(closingCell);
            document.add(closing);

        } catch (DocumentException e) {
            throw new IllegalStateException("Unable to render invoice PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }

        return out.toByteArray();
    }

    // -------------------------------------------------------------------- meta

    private PdfPTable metaBlock(InvoiceDocumentData data) {

        PdfPTable table = new PdfPTable(new float[]{2.3f, 1.2f, 2.3f, 1.4f, 1.3f});
        table.setWidthPercentage(100);

        metaCell(table, "Invoice No", data.invoiceNumber());
        metaCell(table, "Invoice Date",
                data.invoiceDate() == null ? "-" : data.invoiceDate().format(DATE_FORMAT));
        metaCell(table, "Job Card No", data.jobCardNumber());
        metaCell(table, "Vehicle No", data.vehicleNumber());
        metaCell(table, "Service Advisor", nullSafe(data.serviceAdvisor(), "-"));

        table.setSpacingAfter(6);

        return table;
    }

    private void metaCell(PdfPTable table, String label, String value) {
        labelledCell(table, label, value);
    }

    private PdfPTable partiesBlock(InvoiceDocumentData data) {

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        PdfPCell customer = boxCell("CUSTOMER DETAILS");
        customer.addElement(new Paragraph(nullSafe(data.customerName(), "-"), BOLD));
        if (!isBlank(data.customerMobile())) {
            customer.addElement(new Paragraph("Mobile: " + data.customerMobile(), NORMAL));
        }
        if (!isBlank(data.customerAddress())) {
            customer.addElement(new Paragraph(data.customerAddress(), NORMAL));
        }
        table.addCell(customer);

        PdfPCell vehicle = boxCell("VEHICLE DETAILS");
        vehicle.addElement(new Paragraph(nullSafe(data.vehicleDescription(), "-"), BOLD));
        if (!isBlank(data.vehicleNumber())) {
            vehicle.addElement(new Paragraph("Reg. No: " + data.vehicleNumber(), NORMAL));
        }
        if (!isBlank(data.vehicleDetails())) {
            vehicle.addElement(new Paragraph(data.vehicleDetails(), NORMAL));
        }
        table.addCell(vehicle);

        table.setSpacingAfter(8);

        return table;
    }

    // ------------------------------------------------------------- line tables

    private PdfPTable lineSection(
            String title,
            String nameHeader,
            String codeHeader,
            String code,
            List<LineResult> lines) throws DocumentException {

        PdfPTable table = new PdfPTable(new float[]{3.2f, 1f, 0.8f, 1.3f, 1.5f, 1.3f, 1.5f});
        table.setWidthPercentage(100);
        table.setHeaderRows(2);

        PdfPCell banner = new PdfPCell(new Phrase(title, SECTION));
        banner.setColspan(7);
        banner.setBackgroundColor(GREEN);
        banner.setPadding(4);
        banner.setBorder(Rectangle.NO_BORDER);
        table.addCell(banner);

        for (String h : new String[]{nameHeader, codeHeader, "Qty", "Unit Price", "Taxable Value", "GST", "Total"}) {
            table.addCell(headCell(h, h.equals(nameHeader) || h.equals(codeHeader)
                    ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT));
        }

        if (lines.isEmpty()) {
            PdfPCell none = new PdfPCell(new Phrase("No items", SMALL));
            none.setColspan(7);
            none.setBorderColor(BORDER);
            none.setPadding(4);
            table.addCell(none);
        }

        for (LineResult line : lines) {
            table.addCell(bodyCell(line.description(), Element.ALIGN_LEFT));
            table.addCell(bodyCell(code, Element.ALIGN_LEFT));
            table.addCell(bodyCell(quantity(line.quantity()), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(money(line.unitPrice()), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(money(line.taxableValue()), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(money(line.gst()), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(money(line.total()), Element.ALIGN_RIGHT));
        }

        table.setSpacingAfter(8);

        return table;
    }

    private PdfPTable othersSection(List<LineResult> lines) throws DocumentException {

        PdfPTable table = new PdfPTable(new float[]{4.5f, 1f, 1.5f, 1.8f});
        table.setWidthPercentage(100);

        PdfPCell banner = new PdfPCell(new Phrase("OTHER CHARGES (no GST)", SECTION));
        banner.setColspan(4);
        banner.setBackgroundColor(GREEN);
        banner.setPadding(4);
        banner.setBorder(Rectangle.NO_BORDER);
        table.addCell(banner);

        for (String h : new String[]{"Item", "Qty", "Unit Price", "Total"}) {
            table.addCell(headCell(h, "Item".equals(h) ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT));
        }

        for (LineResult line : lines) {
            table.addCell(bodyCell(line.description(), Element.ALIGN_LEFT));
            table.addCell(bodyCell(quantity(line.quantity()), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(money(line.unitPrice()), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(money(line.total()), Element.ALIGN_RIGHT));
        }

        table.setSpacingAfter(8);

        return table;
    }

    // ----------------------------------------------------------------- summary

    private PdfPTable summaryBlock(Breakdown b, InvoiceDocumentData data) throws DocumentException {

        PdfPTable outer = new PdfPTable(new float[]{1f, 1f});
        outer.setWidthPercentage(100);
        outer.setSpacingAfter(6);

        // The left half holds amount in words, payment status and remarks, so the
        // page is not spent on an empty column beside the summary.
        PdfPCell left = noBorder();
        left.setPaddingRight(8);
        left.addElement(wordsBox(b.grandTotal()));
        if (!isBlank(data.paymentStatus())) {
            left.addElement(paymentBox(data.paymentStatus()));
        }
        if (!isBlank(data.remarks())) {
            Paragraph remarks = new Paragraph("Remarks: " + data.remarks(), SMALL);
            remarks.setSpacingBefore(6);
            left.addElement(remarks);
        }
        outer.addCell(left);

        PdfPTable summary = new PdfPTable(new float[]{3f, 1.6f});
        summary.setWidthPercentage(100);

        PdfPCell heading = new PdfPCell(new Phrase("SUMMARY", SECTION));
        heading.setColspan(2);
        heading.setBackgroundColor(NAVY);
        heading.setPadding(4);
        heading.setBorder(Rectangle.NO_BORDER);
        summary.addCell(heading);

        List<String[]> rows = new ArrayList<>();

        rows.add(new String[]{"Parts Total (incl. GST)", money(b.partsTotal())});
        rows.add(new String[]{"   Parts Taxable Value", money(b.partsTaxable())});
        rows.add(new String[]{"Labour / Services (excl. GST)", money(b.labourSubtotal())});

        if (b.othersTotal().signum() != 0) {
            rows.add(new String[]{"Other Charges", money(b.othersTotal())});
        }

        if (b.discount().signum() != 0) {
            rows.add(new String[]{"Discount", "- " + money(b.discount())});
        }

        rows.add(new String[]{"GST included in parts", money(b.partsGst())});
        rows.add(new String[]{"GST on labour @ " + pct(b.gstRatePercent()) + "%", money(b.labourGst())});
        rows.add(new String[]{"CGST @ " + pct(b.gstRatePercent().divide(new BigDecimal("2"))) + "%", money(b.cgst())});
        rows.add(new String[]{"SGST @ " + pct(b.gstRatePercent().divide(new BigDecimal("2"))) + "%", money(b.sgst())});

        for (String[] row : rows) {
            summary.addCell(bodyCell(row[0], Element.ALIGN_LEFT));
            summary.addCell(bodyCell(row[1], Element.ALIGN_RIGHT));
        }

        summary.addCell(bodyCell("Round Off", Element.ALIGN_LEFT));
        summary.addCell(bodyCell(signedMoney(b.roundOff()), Element.ALIGN_RIGHT));

        PdfPCell grandLabel = new PdfPCell(new Phrase("GRAND TOTAL", GRAND));
        grandLabel.setBackgroundColor(HEADER_BG);
        grandLabel.setBorderColor(BORDER);
        grandLabel.setPadding(5);
        summary.addCell(grandLabel);

        PdfPCell grandValue = new PdfPCell(new Phrase(money(b.grandTotal()), GRAND));
        grandValue.setBackgroundColor(HEADER_BG);
        grandValue.setBorderColor(BORDER);
        grandValue.setPadding(5);
        grandValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summary.addCell(grandValue);

        PdfPCell holder = noBorder();
        holder.addElement(summary);
        outer.addCell(holder);

        return outer;
    }

    // ------------------------------------------- invoices with a persisted tax snapshot

    private PdfPTable supplyBlock(InvoiceSnapshotView snap) {

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(6);

        String place = isBlank(snap.placeOfSupplyState()) ? "not recorded" : snap.placeOfSupplyState();
        String supply = snap.supplyType() == SupplyType.INTER_STATE
                ? "inter-state supply (IGST)"
                : "intra-state supply (CGST + SGST)";

        PdfPCell cell = noBorder();
        cell.addElement(new Paragraph("Place of supply: " + place + " - " + supply, SMALL));
        table.addCell(cell);

        return table;
    }

    private PdfPTable snapshotLines(InvoiceSnapshotView snap) throws DocumentException {

        PdfPTable table = new PdfPTable(new float[]{2.9f, 1.6f, 1.3f, 1.3f, 0.9f, 1.5f, 1.3f, 1.5f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(8);

        PdfPCell banner = new PdfPCell(new Phrase("ITEMS (parts: price includes GST; services: GST added on price)", SECTION));
        banner.setColspan(8);
        banner.setBackgroundColor(GREEN);
        banner.setPadding(4);
        banner.setBorder(Rectangle.NO_BORDER);
        table.addCell(banner);

        String[] headers = {"Description", "HSN/SAC", "Qty", "Unit Price", "GST %", "Taxable Value", "GST", "Total"};
        for (int i = 0; i < headers.length; i++) {
            table.addCell(headCell(headers[i], i < 2 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT));
        }

        if (snap.lines().isEmpty()) {
            PdfPCell none = new PdfPCell(new Phrase("No items", SMALL));
            none.setColspan(8);
            none.setBorderColor(BORDER);
            none.setPadding(4);
            table.addCell(none);
        }

        for (InvoiceSnapshotView.Line line : snap.lines()) {
            String qty = quantity(line.quantity()) + (isBlank(line.unit()) ? "" : " " + line.unit());
            table.addCell(bodyCell(line.description(), Element.ALIGN_LEFT));
            table.addCell(bodyCell(isBlank(line.hsnSacCode()) ? NO_CODE : line.hsnSacCode(), Element.ALIGN_LEFT));
            table.addCell(bodyCell(qty, Element.ALIGN_RIGHT));
            table.addCell(bodyCell(money(line.unitPrice()), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(line.ratePercent() == null ? "-" : pct(line.ratePercent()) + "%", Element.ALIGN_RIGHT));
            table.addCell(bodyCell(money(line.taxableValue()), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(money(line.taxAmount()), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(money(line.lineTotal()), Element.ALIGN_RIGHT));
        }

        return table;
    }

    private PdfPTable snapshotSummary(InvoiceSnapshotView snap, InvoiceDocumentData data) throws DocumentException {

        PdfPTable outer = new PdfPTable(new float[]{1f, 1f});
        outer.setWidthPercentage(100);
        outer.setSpacingAfter(6);

        PdfPCell left = noBorder();
        left.setPaddingRight(8);
        left.addElement(wordsBox(snap.grandTotal()));
        if (!isBlank(data.paymentStatus())) {
            left.addElement(paymentBox(data.paymentStatus()));
        }
        if (!isBlank(data.remarks())) {
            Paragraph remarks = new Paragraph("Remarks: " + data.remarks(), SMALL);
            remarks.setSpacingBefore(6);
            left.addElement(remarks);
        }
        outer.addCell(left);

        PdfPTable summary = new PdfPTable(new float[]{3f, 1.6f});
        summary.setWidthPercentage(100);

        PdfPCell heading = new PdfPCell(new Phrase("SUMMARY", SECTION));
        heading.setColspan(2);
        heading.setBackgroundColor(NAVY);
        heading.setPadding(4);
        heading.setBorder(Rectangle.NO_BORDER);
        summary.addCell(heading);

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Items Total (as entered)", money(snap.subtotal())});
        if (snap.discount().signum() != 0) {
            rows.add(new String[]{"Discount", "- " + money(snap.discount())});
        }
        rows.add(new String[]{"Taxable Value", money(snap.taxableTotal())});
        if (snap.supplyType() == SupplyType.INTER_STATE) {
            rows.add(new String[]{"IGST", money(snap.igstTotal())});
        } else {
            rows.add(new String[]{"CGST", money(snap.cgstTotal())});
            rows.add(new String[]{"SGST", money(snap.sgstTotal())});
        }
        rows.add(new String[]{"Total GST", money(snap.taxTotal())});

        for (String[] row : rows) {
            summary.addCell(bodyCell(row[0], Element.ALIGN_LEFT));
            summary.addCell(bodyCell(row[1], Element.ALIGN_RIGHT));
        }

        summary.addCell(bodyCell("Round Off", Element.ALIGN_LEFT));
        summary.addCell(bodyCell(signedMoney(snap.roundOff()), Element.ALIGN_RIGHT));

        PdfPCell grandLabel = new PdfPCell(new Phrase("GRAND TOTAL", GRAND));
        grandLabel.setBackgroundColor(HEADER_BG);
        grandLabel.setBorderColor(BORDER);
        grandLabel.setPadding(5);
        summary.addCell(grandLabel);

        PdfPCell grandValue = new PdfPCell(new Phrase(money(snap.grandTotal()), GRAND));
        grandValue.setBackgroundColor(HEADER_BG);
        grandValue.setBorderColor(BORDER);
        grandValue.setPadding(5);
        grandValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summary.addCell(grandValue);

        PdfPCell holder = noBorder();
        holder.addElement(summary);
        outer.addCell(holder);

        return outer;
    }

    private PdfPTable wordsBox(BigDecimal grandTotal) {

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        cell.addElement(new Paragraph("Amount in words", SMALL));
        cell.addElement(new Paragraph(AmountInWords.rupees(grandTotal), BOLD));

        table.addCell(cell);

        return table;
    }

    /** Embeds the garage authorization image (right-aligned); an unreadable image is skipped, never fatal. */
    private static void addAuthorizationImage(PdfPCell cell, byte[] bytes, float maxWidth, float maxHeight) {

        if (bytes == null) {
            return;
        }

        try {
            com.lowagie.text.Image image = com.lowagie.text.Image.getInstance(bytes);
            image.scaleToFit(maxWidth, maxHeight);
            image.setAlignment(Element.ALIGN_RIGHT);
            cell.addElement(image);
        } catch (Exception e) {
            // validated at upload; a corrupt object falls back to the blank signatory line
        }
    }

    private PdfPTable signatureBlock(InvoiceDocumentData data) {

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(14);

        PdfPCell customer = noBorder();
        customer.setPaddingTop(16);
        customer.addElement(new Paragraph("______________________", NORMAL));
        customer.addElement(new Paragraph("Customer Signature", SMALL));
        table.addCell(customer);

        PdfPCell garage = noBorder();
        garage.setPaddingTop(16);
        if (data.stampBytes() == null && data.signatureBytes() != null) {
            // combined signature + stamp image
            addAuthorizationImage(garage, data.signatureBytes(), 170, 90);
        } else {
            addAuthorizationImage(garage, data.stampBytes(), 90, 90);
            addAuthorizationImage(garage, data.signatureBytes(), 140, 50);
        }
        Paragraph line = new Paragraph("______________________", NORMAL);
        line.setAlignment(Element.ALIGN_RIGHT);
        garage.addElement(line);
        Paragraph sign = new Paragraph(
                "Authorised Signatory - " + nullSafe(data.garage().getGarageName(), "Garage"), SMALL);
        sign.setAlignment(Element.ALIGN_RIGHT);
        garage.addElement(sign);
        table.addCell(garage);

        return table;
    }

    // ----------------------------------------------------------------- helpers

    /**
     * Shown only when the invoice actually carries a payment status. The
     * domain records status only (no method / reference / date), so none of
     * those are printed.
     */
    private PdfPTable paymentBox(String paymentStatus) {

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);

        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        cell.addElement(new Paragraph("Payment status", SMALL));
        cell.addElement(new Paragraph(paymentStatus.replace('_', ' '), BOLD));

        table.addCell(cell);

        return table;
    }

    private static String pct(BigDecimal rate) {
        return rate.stripTrailingZeros().toPlainString();
    }

    private static String signedMoney(BigDecimal value) {
        if (value.signum() == 0) {
            return "₹ 0.00";
        }

        return (value.signum() < 0 ? "- " : "+ ") + "₹ " + plain(value.abs());
    }

    private static String quantity(BigDecimal quantity) {
        return quantity == null ? "" : quantity.stripTrailingZeros().toPlainString();
    }
}
