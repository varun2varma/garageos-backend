package com.garageos.core.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Footer drawn on every page in the bottom margin, outside the content flow.
 * Because it is not part of the document body it can never spill onto a page
 * of its own (the cause of the old blank trailing invoice page).
 *
 * Left: a document note (e.g. "This is a computer-generated tax invoice.").
 * Centre: "Page X of Y". Right: "Powered by GarageST" and an optional
 * document/version reference.
 */
public class GarageStPageFooter extends PdfPageEventHelper {

    private final String note;
    private final String reference;
    private PdfTemplate total;

    public GarageStPageFooter(String note, String reference) {
        this.note = note;
        this.reference = reference;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        total = writer.getDirectContent().createTemplate(30, 12);
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {

        PdfContentByte cb = writer.getDirectContent();

        float left = document.left();
        float right = document.right();
        float y = document.bottom() - 14;

        cb.setColorStroke(GarageStPdf.BORDER);
        cb.setLineWidth(0.5f);
        cb.moveTo(left, y + 10);
        cb.lineTo(right, y + 10);
        cb.stroke();

        if (note != null && !note.isBlank()) {
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(note, GarageStPdf.SMALL), left, y, 0);
        }

        String pageText = "Page " + writer.getPageNumber() + " of ";
        float center = (left + right) / 2;
        float textWidth = GarageStPdf.SMALL.getBaseFont().getWidthPoint(pageText, GarageStPdf.SMALL.getSize());

        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(pageText, GarageStPdf.SMALL), center - 10, y - 11, 0);
        cb.addTemplate(total, center - 10 + textWidth, y - 11);

        String brand = GarageStPdf.isBlank(reference)
                ? "Powered by GarageST"
                : "Powered by GarageST  |  " + reference;
        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                new Phrase(brand, GarageStPdf.SMALL), right, y - 11, 0);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        // Page count is known only now; fill the "Y" of "Page X of Y".
        total.beginText();
        total.setFontAndSize(GarageStPdf.SMALL.getBaseFont(), GarageStPdf.SMALL.getSize());
        total.setColorFill(java.awt.Color.DARK_GRAY);
        total.setTextMatrix(0, 0);
        total.showText(String.valueOf(writer.getPageNumber() - 1));
        total.endText();
    }

    /** Bottom margin that leaves room for this footer. */
    public static final float BOTTOM_MARGIN = 52;
}
