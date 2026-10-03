package com.garageos.modules.invoice.pdf;

import com.garageos.core.util.AmountInWords;
import com.garageos.core.util.InvoiceTaxCalculator.Breakdown;
import com.garageos.core.util.InvoiceTaxCalculator.LineResult;
import com.garageos.modules.garage.dto.response.GarageBrandingResponse;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the GarageST TAX INVOICE PDF from {@link InvoiceDocumentData}.
 *
 * Layout (top to bottom): garage logo + name/address/contact/GSTIN, TAX
 * INVOICE title, invoice/job-card/vehicle/advisor meta, customer + vehicle
 * blocks, PARTS table, LABOUR / SERVICES table, optional OTHER CHARGES,
 * tax summary (CGST/SGST/round-off/grand total), amount in words, and
 * signature areas. Nothing garage-specific is hardcoded.
 *
 * Money is printed with the real rupee sign using an embedded Noto Sans font.
 */
@Component
public class InvoicePdfRenderer {

    /** Default HSN for motor-vehicle parts and SAC for motor-vehicle repair services (not garage-specific). */
    static final String DEFAULT_PART_HSN = "8708";
    static final String DEFAULT_LABOUR_SAC = "9987";

    private static final Color NAVY = new Color(0x05, 0x1C, 0x32);
    private static final Color GREEN = new Color(0x12, 0x94, 0x65);
    private static final Color HEADER_BG = new Color(0xE8, 0xEE, 0xF4);
    private static final Color BORDER = new Color(0xB8, 0xC2, 0xCC);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // Embedded Noto Sans (SIL OFL 1.1, see resources/fonts/OFL.txt): the
    // built-in PDF fonts have no rupee glyph, this one does.
    private static final BaseFont REGULAR_BASE = loadBase("fonts/NotoSans-Regular.ttf", FontFactory.HELVETICA);
    private static final BaseFont BOLD_BASE = loadBase("fonts/NotoSans-Bold.ttf", FontFactory.HELVETICA_BOLD);

    private static final Font GARAGE_NAME = new Font(BOLD_BASE, 16, Font.NORMAL, NAVY);
    private static final Font SECTION = new Font(BOLD_BASE, 10, Font.NORMAL, Color.WHITE);
    private static final Font NORMAL = new Font(REGULAR_BASE, 9, Font.NORMAL, Color.BLACK);
    private static final Font SMALL = new Font(REGULAR_BASE, 8, Font.NORMAL, Color.DARK_GRAY);
    private static final Font BOLD = new Font(BOLD_BASE, 9, Font.NORMAL, Color.BLACK);
    private static final Font TABLE_HEAD = new Font(BOLD_BASE, 8, Font.NORMAL, NAVY);
    private static final Font GRAND = new Font(BOLD_BASE, 11, Font.NORMAL, NAVY);
    private static final Font BANNER = new Font(BOLD_BASE, 14, Font.NORMAL, Color.WHITE);

    private static BaseFont loadBase(String resource, String fallbackBuiltIn) {
        try (InputStream in = new ClassPathResource(resource).getInputStream()) {
            return BaseFont.createFont(resource, BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    true, in.readAllBytes(), null);
        } catch (IOException | RuntimeException e) {
            // Never fail an invoice over a font: fall back to a built-in font.
            try {
                return BaseFont.createFont(fallbackBuiltIn, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (IOException | RuntimeException ex) {
                throw new IllegalStateException("No usable PDF font", ex);
            }
        }
    }

    public byte[] render(InvoiceDocumentData data) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4, 32, 32, 28, 28);

        try {
            PdfWriter.getInstance(document, out);

            document.open();

            document.add(headerBlock(data));
            document.add(titleBar());
            document.add(metaBlock(data));
            document.add(partiesBlock(data));

            Breakdown b = data.breakdown();

            document.add(lineSection(
                    "PARTS (prices are GST-inclusive)",
                    "Part", "HSN", DEFAULT_PART_HSN, b.parts()));

            document.add(lineSection(
                    "LABOUR / SERVICES (GST added on price)",
                    "Service", "SAC", DEFAULT_LABOUR_SAC, b.labour()));

            if (!b.others().isEmpty()) {
                document.add(othersSection(b.others()));
            }

            document.add(summaryBlock(b));
            document.add(wordsBlock(b));

            if (data.remarks() != null && !data.remarks().isBlank()) {
                Paragraph remarks = new Paragraph("Remarks: " + data.remarks(), SMALL);
                remarks.setSpacingBefore(6);
                document.add(remarks);
            }

            document.add(signatureBlock(data));

            Paragraph footer = new Paragraph(
                    "This is a computer-generated tax invoice.", SMALL);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(10);
            document.add(footer);

        } catch (DocumentException e) {
            throw new IllegalStateException("Unable to render invoice PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }

        return out.toByteArray();
    }

    // ------------------------------------------------------------------ header

    private PdfPTable headerBlock(InvoiceDocumentData data) throws DocumentException {

        PdfPTable table = new PdfPTable(new float[]{1.2f, 4f});
        table.setWidthPercentage(100);

        PdfPCell logoCell = noBorder();
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Image logo = loadLogo(data.logoBytes());

        if (logo != null) {
            logo.scaleToFit(80, 70);
            logoCell.addElement(logo);
        }

        table.addCell(logoCell);

        GarageBrandingResponse g = data.garage();

        PdfPCell info = noBorder();

        info.addElement(new Paragraph(nullSafe(g.getGarageName(), "Garage"), GARAGE_NAME));

        String addressLine = join(", ", g.getAddress(), g.getLandmark());
        String cityLine = join(", ", g.getCity(), g.getState())
                + (isBlank(g.getPincode()) ? "" : " - " + g.getPincode());

        if (!isBlank(addressLine)) {
            info.addElement(new Paragraph(addressLine, NORMAL));
        }

        if (!isBlank(cityLine)) {
            info.addElement(new Paragraph(cityLine, NORMAL));
        }

        String contact = join("   |   ",
                isBlank(g.getPhone()) ? null : "Phone: " + g.getPhone(),
                isBlank(g.getEmail()) ? null : "Email: " + g.getEmail());

        if (!isBlank(contact)) {
            info.addElement(new Paragraph(contact, NORMAL));
        }

        if (!isBlank(g.getGstNumber())) {
            info.addElement(new Paragraph("GSTIN: " + g.getGstNumber(), BOLD));
        }

        table.addCell(info);

        table.setSpacingAfter(6);

        return table;
    }

    private PdfPTable titleBar() {

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell(new Phrase("TAX INVOICE",
                BANNER));
        cell.setBackgroundColor(NAVY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        cell.setBorder(Rectangle.NO_BORDER);

        table.addCell(cell);
        table.setSpacingAfter(6);

        return table;
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

        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER);
        cell.setPadding(4);

        if (!label.isEmpty()) {
            cell.addElement(new Paragraph(label, SMALL));
            cell.addElement(new Paragraph(nullSafe(value, "-"), BOLD));
        } else {
            cell.setBorder(Rectangle.NO_BORDER);
        }

        table.addCell(cell);
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

    private PdfPCell boxCell(String heading) {

        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        cell.addElement(new Paragraph(heading, TABLE_HEAD));

        return cell;
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
            PdfPCell head = new PdfPCell(new Phrase(h, TABLE_HEAD));
            head.setBackgroundColor(HEADER_BG);
            head.setBorderColor(BORDER);
            head.setPadding(4);
            head.setHorizontalAlignment(h.equals(nameHeader) || h.equals(codeHeader)
                    ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            table.addCell(head);
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
            PdfPCell head = new PdfPCell(new Phrase(h, TABLE_HEAD));
            head.setBackgroundColor(HEADER_BG);
            head.setBorderColor(BORDER);
            head.setPadding(4);
            head.setHorizontalAlignment("Item".equals(h) ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            table.addCell(head);
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

    private PdfPTable summaryBlock(Breakdown b) throws DocumentException {

        PdfPTable outer = new PdfPTable(new float[]{1f, 1f});
        outer.setWidthPercentage(100);
        outer.setSpacingAfter(6);

        PdfPCell spacer = noBorder();
        outer.addCell(spacer);

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

    private PdfPTable wordsBlock(Breakdown b) {

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        cell.addElement(new Paragraph("Amount in words", SMALL));
        cell.addElement(new Paragraph(AmountInWords.rupees(b.grandTotal()), BOLD));

        table.addCell(cell);

        return table;
    }

    private PdfPTable signatureBlock(InvoiceDocumentData data) {

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(28);

        PdfPCell customer = noBorder();
        customer.setPaddingTop(24);
        customer.addElement(new Paragraph("______________________", NORMAL));
        customer.addElement(new Paragraph("Customer Signature", SMALL));
        table.addCell(customer);

        PdfPCell garage = noBorder();
        garage.setPaddingTop(24);
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

    private PdfPCell bodyCell(String text, int alignment) {

        PdfPCell cell = new PdfPCell(new Phrase(nullSafe(text, ""), NORMAL));
        cell.setBorderColor(BORDER);
        cell.setPadding(4);
        cell.setHorizontalAlignment(alignment);

        return cell;
    }

    private PdfPCell noBorder() {

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);

        return cell;
    }

    /**
     * The garage's own logo when it has one and the bytes are a decodable
     * PNG/JPEG; otherwise (no logo, corrupt file) the GarageST default mark,
     * so an invoice can always be produced.
     */
    private Image loadLogo(byte[] logoBytes) {

        if (logoBytes != null && logoBytes.length > 0) {
            try {
                return Image.getInstance(logoBytes);
            } catch (IOException | RuntimeException ignored) {
                // fall through to the default mark
            }
        }

        try (InputStream in = new ClassPathResource("branding/garagest-mark.png").getInputStream()) {
            return Image.getInstance(in.readAllBytes());
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static String pct(BigDecimal rate) {
        return rate.stripTrailingZeros().toPlainString();
    }

    static String money(BigDecimal value) {
        return "₹ " + plain(value);
    }

    private static String signedMoney(BigDecimal value) {
        if (value.signum() == 0) {
            return "₹ 0.00";
        }

        return (value.signum() < 0 ? "- " : "+ ") + "₹ " + plain(value.abs());
    }

    private static String plain(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String quantity(BigDecimal quantity) {
        return quantity == null ? "" : quantity.stripTrailingZeros().toPlainString();
    }

    private static String nullSafe(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String join(String separator, String... parts) {

        List<String> present = new ArrayList<>();

        for (String part : parts) {
            if (!isBlank(part)) {
                present.add(part.trim());
            }
        }

        return String.join(separator, present);
    }
}
