package com.garageos.core.pdf;

import com.garageos.modules.garage.dto.response.GarageBrandingResponse;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.core.io.ClassPathResource;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * The GarageST PDF visual language, shared by the tax invoice and the final
 * Job Card reports so they read as one product family: same palette
 * (taken from the GarageST mark), same Noto Sans typography (has the rupee
 * glyph), same garage-first header with a "Powered by GarageST" line, same
 * section banners, tables and footer.
 *
 * The garage is always the primary identity; GarageST is the platform.
 */
public final class GarageStPdf {

    private GarageStPdf() {
    }

    public static final Color NAVY = new Color(0x05, 0x1C, 0x32);
    public static final Color GREEN = new Color(0x12, 0x94, 0x65);
    public static final Color HEADER_BG = new Color(0xE8, 0xEE, 0xF4);
    public static final Color BORDER = new Color(0xB8, 0xC2, 0xCC);

    // Embedded Noto Sans (SIL OFL 1.1, see resources/fonts/OFL.txt): the
    // built-in PDF fonts have no rupee glyph, this one does.
    private static final BaseFont REGULAR_BASE = loadBase("fonts/NotoSans-Regular.ttf", FontFactory.HELVETICA);
    private static final BaseFont BOLD_BASE = loadBase("fonts/NotoSans-Bold.ttf", FontFactory.HELVETICA_BOLD);

    public static final Font GARAGE_NAME = new Font(BOLD_BASE, 16, Font.NORMAL, NAVY);
    public static final Font SECTION = new Font(BOLD_BASE, 10, Font.NORMAL, Color.WHITE);
    public static final Font NORMAL = new Font(REGULAR_BASE, 9, Font.NORMAL, Color.BLACK);
    public static final Font SMALL = new Font(REGULAR_BASE, 8, Font.NORMAL, Color.DARK_GRAY);
    public static final Font BOLD = new Font(BOLD_BASE, 9, Font.NORMAL, Color.BLACK);
    public static final Font TABLE_HEAD = new Font(BOLD_BASE, 8, Font.NORMAL, NAVY);
    public static final Font GRAND = new Font(BOLD_BASE, 11, Font.NORMAL, NAVY);
    public static final Font BANNER = new Font(BOLD_BASE, 14, Font.NORMAL, Color.WHITE);
    public static final Font BRAND = new Font(BOLD_BASE, 8, Font.NORMAL, NAVY);
    public static final Font COVER_TITLE = new Font(BOLD_BASE, 22, Font.NORMAL, NAVY);

    private static BaseFont loadBase(String resource, String fallbackBuiltIn) {
        try (InputStream in = new ClassPathResource(resource).getInputStream()) {
            return BaseFont.createFont(resource, BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    true, in.readAllBytes(), null);
        } catch (IOException | RuntimeException e) {
            // Never fail a document over a font: fall back to a built-in font.
            try {
                return BaseFont.createFont(fallbackBuiltIn, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (IOException | RuntimeException ex) {
                throw new IllegalStateException("No usable PDF font", ex);
            }
        }
    }

    // ------------------------------------------------------------ doc metadata

    public static void applyMetadata(Document document, String title, String subject) {
        document.addTitle(title);
        document.addSubject(subject);
        document.addAuthor("GarageST");
        document.addCreator("GarageST");
    }

    // ------------------------------------------------------------------ header

    /**
     * Garage identity (logo, name, address, contact, GSTIN) with a
     * "Powered by GarageST" line underneath. The garage's own logo when it
     * has a decodable one; otherwise the GarageST mark so a document can
     * always be produced.
     */
    public static PdfPTable garageHeader(GarageBrandingResponse g, byte[] logoBytes) {

        PdfPTable table = new PdfPTable(new float[]{1.2f, 4f});
        table.setWidthPercentage(100);

        PdfPCell logoCell = noBorder();
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Image logo = loadLogo(logoBytes);

        if (logo != null) {
            logo.scaleToFit(80, 70);
            logoCell.addElement(logo);
        }

        table.addCell(logoCell);

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

        PdfPCell poweredBy = noBorder();
        poweredBy.setColspan(2);
        poweredBy.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph powered = new Paragraph("Powered by GARAGEST", BRAND);
        powered.setAlignment(Element.ALIGN_RIGHT);
        poweredBy.addElement(powered);
        table.addCell(poweredBy);

        table.setSpacingAfter(6);

        return table;
    }

    public static PdfPTable titleBar(String title) {

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell(new Phrase(title, BANNER));
        cell.setBackgroundColor(NAVY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        cell.setBorder(Rectangle.NO_BORDER);

        table.addCell(cell);
        table.setSpacingAfter(6);

        return table;
    }

    /** Full-width green section banner followed by nothing; callers add their own table. */
    public static PdfPTable sectionHeader(String title) {

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell banner = new PdfPCell(new Phrase(title, SECTION));
        banner.setBackgroundColor(GREEN);
        banner.setPadding(4);
        banner.setBorder(Rectangle.NO_BORDER);
        table.addCell(banner);
        table.setSpacingBefore(4);
        // Keep the banner with the table that follows it.
        table.setKeepTogether(true);

        return table;
    }

    // ------------------------------------------------------------------- cells

    public static PdfPCell bodyCell(String text, int alignment) {

        PdfPCell cell = new PdfPCell(new Phrase(nullSafe(text, ""), NORMAL));
        cell.setBorderColor(BORDER);
        cell.setPadding(4);
        cell.setHorizontalAlignment(alignment);

        return cell;
    }

    public static PdfPCell headCell(String text, int alignment) {

        PdfPCell head = new PdfPCell(new Phrase(text, TABLE_HEAD));
        head.setBackgroundColor(HEADER_BG);
        head.setBorderColor(BORDER);
        head.setPadding(4);
        head.setHorizontalAlignment(alignment);

        return head;
    }

    public static PdfPCell noBorder() {

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);

        return cell;
    }

    public static PdfPCell boxCell(String heading) {

        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        cell.addElement(new Paragraph(heading, TABLE_HEAD));

        return cell;
    }

    /** One labelled value box, as used in the invoice meta strip. */
    public static void labelledCell(PdfPTable table, String label, String value) {

        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER);
        cell.setPadding(4);
        cell.addElement(new Paragraph(label, SMALL));
        cell.addElement(new Paragraph(nullSafe(value, "-"), BOLD));

        table.addCell(cell);
    }

    // ------------------------------------------------------------------ images

    public static Image loadLogo(byte[] logoBytes) {

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

    // ------------------------------------------------------------------- money

    public static String money(BigDecimal value) {
        return "₹ " + plain(value);
    }

    public static String plain(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    // ------------------------------------------------------------------ strings

    public static String nullSafe(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static String join(String separator, String... parts) {

        List<String> present = new ArrayList<>();

        for (String part : parts) {
            if (!isBlank(part)) {
                present.add(part.trim());
            }
        }

        return String.join(separator, present);
    }
}
