package com.garageos.modules.finalreport.pdf;

import com.garageos.core.enums.report.FinalReportAudience;
import com.garageos.core.pdf.GarageStPageFooter;
import com.garageos.core.util.InvoiceTaxCalculator.Breakdown;
import com.garageos.core.util.InvoiceTaxCalculator.LineResult;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO.FindingInfo;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO.PhotoInfo;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO.RepairInfo;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO.TechnicianInfo;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import com.garageos.core.util.DisplayTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.garageos.core.pdf.GarageStPdf.*;

/**
 * Renders the final Job Card service report from a {@link JobCardFinalReportDTO}.
 * The renderer is audience-agnostic: it prints whatever the projection left in
 * the DTO and omits any section with no data, so an owner-only field can never
 * appear in a customer report (it simply is not there to print).
 */
@Component
public class FinalReportPdfRenderer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private static final String CUSTOMER_DISCLAIMER =
            "This report summarises the service carried out on your vehicle as recorded by the workshop. "
                    + "Findings reflect the vehicle's condition at the time of inspection. Photographs are "
                    + "provided for reference. For the tax invoice, refer to the separately issued invoice.";

    private static final String OWNER_DISCLAIMER =
            "OWNER COPY - INTERNAL. This record contains operational information and must not be shared "
                    + "with the customer. A separate customer report is issued for the customer.";

    public byte[] render(JobCardFinalReportDTO d) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 32, 32, 28, GarageStPageFooter.BOTTOM_MARGIN);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new GarageStPageFooter(
                    d.audience() == FinalReportAudience.OWNER
                            ? "Owner copy - internal"
                            : "Customer copy",
                    d.reference()));

            applyMetadata(document,
                    "Service Report " + d.job().jobCardNumber(),
                    d.audience() + " report v" + d.version());

            document.open();

            document.add(garageHeader(d.garage(), d.logoBytes()));
            document.add(titleBar("VEHICLE SERVICE REPORT"));
            document.add(audienceLine(d));
            document.add(summaryStrip(d));
            document.add(outcomeBand(d));
            document.add(parties(d));

            complaints(document, d);
            declinedComplaints(document, d);
            inspection(document, d);
            approvedWork(document, d);
            repairs(document, d);
            quality(document, d);
            invoiceAndPayment(document, d);
            delivery(document, d);
            ownerWorkshop(document, d);
            photos(document, d);
            tripEvidence(document, d);

            document.add(disclaimer(d));

        } catch (DocumentException e) {
            throw new IllegalStateException("Unable to render final report PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }

        return out.toByteArray();
    }

    // ----------------------------------------------------------------- header

    private Paragraph audienceLine(JobCardFinalReportDTO d) {
        String text = d.audience() == FinalReportAudience.OWNER
                ? "OWNER REPORT - complete workshop record"
                : "CUSTOMER REPORT - your service history";
        Paragraph p = new Paragraph(text, TABLE_HEAD);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(6);
        return p;
    }

    private PdfPTable summaryStrip(JobCardFinalReportDTO d) {

        PdfPTable table = new PdfPTable(new float[]{2.4f, 1.4f, 1.4f, 2.4f});
        table.setWidthPercentage(100);

        labelledCell(table, "Job Card No", d.job().jobCardNumber());
        labelledCell(table, "Service Date", date(d.job().serviceDate()));
        labelledCell(table, "Odometer In", d.job().odometerIn() == null ? "-" : d.job().odometerIn() + " km");
        labelledCell(table, "Report", d.reference());
        labelledCell(table, "Service Advisor",
                d.job().serviceAdvisor() == null || d.job().serviceAdvisor().isBlank()
                        ? com.garageos.core.util.StaffAttribution.NOT_RECORDED
                        : d.job().serviceAdvisor());
        table.completeRow();

        table.setSpacingAfter(6);
        return table;
    }

    /**
     * "At a glance": the outcome of the visit in four facts, all derived from data already in the report (nothing new
     * is asserted): quality result, tasks completed, total recorded work time, and the handover date.
     */
    private PdfPTable outcomeBand(JobCardFinalReportDTO d) {

        List<JobCardFinalReportDTO.RepairInfo> repairs = d.repairs() == null ? List.of() : d.repairs();
        long done = repairs.stream().filter(r -> "COMPLETED".equals(r.status())).count();
        double hours = repairs.stream().map(JobCardFinalReportDTO.RepairInfo::actualHours)
                .filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).sum();
        boolean anyTime = repairs.stream().anyMatch(r -> r.actualHours() != null);

        PdfPTable table = new PdfPTable(new float[]{1.4f, 1.4f, 1.4f, 1.8f});
        table.setWidthPercentage(100);

        labelledCell(table, "Quality check",
                d.qualityCheck() == null ? "-" : label(d.qualityCheck().status()));
        labelledCell(table, "Work completed", repairs.isEmpty() ? "-" : done + " of " + repairs.size());
        labelledCell(table, "Total work time",
                anyTime ? com.garageos.core.util.ActualDuration.format(hours) : "-");
        labelledCell(table, "Handed over",
                d.delivery() == null ? "Not yet" : dateTime(d, d.delivery().deliveredAt()));
        table.completeRow();

        table.setSpacingAfter(6);
        return table;
    }

    private PdfPTable parties(JobCardFinalReportDTO d) {

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        PdfPCell customer = boxCell("CUSTOMER");
        customer.addElement(new Paragraph(nullSafe(d.customer().name(), "-"), BOLD));
        if (!isBlank(d.customer().mobile())) {
            customer.addElement(new Paragraph("Mobile: " + d.customer().mobile(), NORMAL));
        }
        if (!isBlank(d.customer().address())) {
            customer.addElement(new Paragraph(d.customer().address(), NORMAL));
        }
        table.addCell(customer);

        PdfPCell vehicle = boxCell("VEHICLE");
        vehicle.addElement(new Paragraph(nullSafe(d.vehicle().description(), "-"), BOLD));
        if (!isBlank(d.vehicle().registrationNumber())) {
            vehicle.addElement(new Paragraph("Reg. No: " + d.vehicle().registrationNumber(), NORMAL));
        }
        if (!isBlank(d.vehicle().details())) {
            vehicle.addElement(new Paragraph(d.vehicle().details(), NORMAL));
        }
        table.addCell(vehicle);

        table.setSpacingAfter(6);
        return table;
    }

    // ---------------------------------------------------------------- sections

    private void complaints(Document doc, JobCardFinalReportDTO d) throws DocumentException {

        if (d.complaints() == null || d.complaints().isEmpty()) {
            return;
        }

        PdfPTable t = section("CUSTOMER COMPLAINTS", new float[]{0.6f, 8f}, "#", "Complaint");

        int i = 1;
        for (String c : d.complaints()) {
            t.addCell(bodyCell(String.valueOf(i++), Element.ALIGN_LEFT));
            t.addCell(bodyCell(c, Element.ALIGN_LEFT));
        }

        doc.add(t);
    }

    /** Complaints the customer removed from this visit: listed as NOT performed, never as completed work. */
    private void declinedComplaints(Document doc, JobCardFinalReportDTO d) throws DocumentException {

        if (d.declinedComplaints() == null || d.declinedComplaints().isEmpty()) {
            return;
        }

        PdfPTable t = section("REMOVED AT THE CUSTOMER'S REQUEST (NOT PERFORMED)", new float[]{0.6f, 8f},
                "#", "Complaint - removed from this service visit");

        int i = 1;
        for (String c : d.declinedComplaints()) {
            t.addCell(bodyCell(String.valueOf(i++), Element.ALIGN_LEFT));
            t.addCell(bodyCell(c, Element.ALIGN_LEFT));
        }

        doc.add(t);
    }

    private void inspection(Document doc, JobCardFinalReportDTO d) throws DocumentException {

        JobCardFinalReportDTO.InspectionInfo ins = d.inspection();

        if (ins == null) {
            return;
        }

        List<FindingInfo> findings = ins.findings() == null ? List.of() : ins.findings();

        if (!findings.isEmpty()) {
            long pass = findings.stream().filter(f -> "PASS".equals(f.status())).count();
            long attention = findings.size() - pass;

            PdfPTable summary = section("INSPECTION SUMMARY", new float[]{1f, 1f, 1f},
                    "Items checked", "OK", "Needs attention / repaired");
            summary.addCell(bodyCell(String.valueOf(findings.size()), Element.ALIGN_CENTER));
            summary.addCell(bodyCell(String.valueOf(pass), Element.ALIGN_CENTER));
            summary.addCell(bodyCell(String.valueOf(attention), Element.ALIGN_CENTER));
            doc.add(summary);

            boolean withRemarks = d.audience() == FinalReportAudience.OWNER
                    && findings.stream().anyMatch(f -> !isBlank(f.remarks()));

            Map<String, List<FindingInfo>> byCategory = new LinkedHashMap<>();
            for (FindingInfo f : findings) {
                byCategory.computeIfAbsent(nullSafe(f.category(), "General"), k -> new java.util.ArrayList<>()).add(f);
            }

            for (Map.Entry<String, List<FindingInfo>> e : byCategory.entrySet()) {
                PdfPTable t = withRemarks
                        ? section(e.getKey().toUpperCase(), new float[]{3f, 1.4f, 3f}, "Check", "Result", "Remarks")
                        : section(e.getKey().toUpperCase(), new float[]{4f, 1.6f}, "Check", "Result");
                for (FindingInfo f : e.getValue()) {
                    t.addCell(bodyCell(f.item(), Element.ALIGN_LEFT));
                    t.addCell(bodyCell(findingLabel(f.status()), Element.ALIGN_LEFT));
                    if (withRemarks) {
                        t.addCell(bodyCell(f.remarks(), Element.ALIGN_LEFT));
                    }
                }
                doc.add(t);
            }
        }

        if (!isBlank(ins.recommendedWork())) {
            String recommended = ins.recommendedWork();
            if (d.declinedComplaints() != null && !d.declinedComplaints().isEmpty()) {
                // The recommendation is the inspector's free text, recorded before the customer removed work; it cannot be
                // matched to complaints reliably, so say plainly (inside the same box) that removal overrides it.
                recommended += "\n\nNote: work listed under \"Removed at the customer's request\" was not authorised and "
                        + "was not performed, even if it was recommended above.";
            }
            doc.add(textBox("RECOMMENDED WORK", recommended));
        }

        if (d.audience() == FinalReportAudience.OWNER && !isBlank(ins.notes())) {
            doc.add(textBox("INSPECTOR NOTES (INTERNAL)", ins.notes()));
        }
    }

    private void approvedWork(Document doc, JobCardFinalReportDTO d) throws DocumentException {

        Breakdown w = d.work();

        if (w == null) {
            return;
        }

        if (!isBlank(d.estimateNumber())) {
            Paragraph p = new Paragraph(
                    "Estimate " + d.estimateNumber()
                            + (isBlank(d.estimateStatus()) ? "" : " - " + d.estimateStatus().replace('_', ' ')),
                    SMALL);
            p.setSpacingBefore(4);
            doc.add(p);
        }

        workTable(doc, "PARTS", w.parts());
        workTable(doc, "LABOUR / SERVICES", w.labour());
        workTable(doc, "OTHER CHARGES", w.others());
    }

    private void workTable(Document doc, String title, List<LineResult> lines) throws DocumentException {

        if (lines == null || lines.isEmpty()) {
            return;
        }

        PdfPTable t = section(title, new float[]{5f, 1f, 1.8f}, "Item", "Qty", "Amount");

        for (LineResult line : lines) {
            t.addCell(bodyCell(line.description(), Element.ALIGN_LEFT));
            t.addCell(bodyCell(line.quantity() == null ? ""
                    : line.quantity().stripTrailingZeros().toPlainString(), Element.ALIGN_RIGHT));
            t.addCell(bodyCell(money(line.total()), Element.ALIGN_RIGHT));
        }

        doc.add(t);
    }

    private void repairs(Document doc, JobCardFinalReportDTO d) throws DocumentException {

        List<RepairInfo> repairs = d.repairs();

        if (repairs == null || repairs.isEmpty()) {
            return;
        }

        boolean owner = d.audience() == FinalReportAudience.OWNER;

        PdfPTable t = owner
                ? section("REPAIR WORK", new float[]{3f, 1.2f, 1.6f, 1.2f, 1.5f, 2f},
                "Work", "Status", "Completed", "Time taken", "Technician", "Remarks")
                : section("REPAIR WORK", new float[]{3.6f, 1.4f, 1.8f, 1.3f}, "Work", "Status", "Completed", "Time taken");

        for (RepairInfo r : repairs) {
            t.addCell(bodyCell(r.description(), Element.ALIGN_LEFT));
            t.addCell(bodyCell(label(r.status()), Element.ALIGN_LEFT));
            t.addCell(bodyCell(dateTime(d, r.completedAt()), Element.ALIGN_LEFT));
            t.addCell(bodyCell(com.garageos.core.util.ActualDuration.format(r.actualHours()), Element.ALIGN_LEFT));
            if (owner) {
                t.addCell(bodyCell(r.technicianName(), Element.ALIGN_LEFT));
                t.addCell(bodyCell(r.remarks(), Element.ALIGN_LEFT));
            }
        }

        doc.add(t);
    }

    private void quality(Document doc, JobCardFinalReportDTO d) throws DocumentException {

        JobCardFinalReportDTO.QualityInfo q = d.qualityCheck();

        if (q == null) {
            return;
        }

        boolean owner = d.audience() == FinalReportAudience.OWNER;

        PdfPTable t = owner
                ? section("QUALITY CHECK", new float[]{1.2f, 1.8f, 1.8f, 3f}, "Result", "Checked on", "Checked by", "Remarks")
                : section("QUALITY CHECK", new float[]{1.5f, 2f}, "Result", "Checked on");

        t.addCell(bodyCell(label(q.status()), Element.ALIGN_LEFT));
        t.addCell(bodyCell(dateTime(d, q.inspectedAt()), Element.ALIGN_LEFT));
        if (owner) {
            t.addCell(bodyCell(q.inspectedBy(), Element.ALIGN_LEFT));
            t.addCell(bodyCell(q.remarks(), Element.ALIGN_LEFT));
        }

        doc.add(t);
    }

    private void invoiceAndPayment(Document doc, JobCardFinalReportDTO d) throws DocumentException {

        JobCardFinalReportDTO.InvoiceInfo inv = d.invoice();

        if (inv == null) {
            return;
        }

        PdfPTable t = section("INVOICE & PAYMENT", new float[]{2f, 1.4f, 1.6f, 1.4f},
                "Invoice No", "Invoice Date", "Amount", "Payment");

        t.addCell(bodyCell(inv.invoiceNumber(), Element.ALIGN_LEFT));
        t.addCell(bodyCell(date(inv.invoiceDate()), Element.ALIGN_LEFT));
        t.addCell(bodyCell(inv.grandTotal() == null ? "-" : money(inv.grandTotal()), Element.ALIGN_RIGHT));
        t.addCell(bodyCell(label(inv.paymentStatus()), Element.ALIGN_LEFT));

        doc.add(t);
    }

    private void delivery(Document doc, JobCardFinalReportDTO d) throws DocumentException {

        JobCardFinalReportDTO.DeliveryInfo del = d.delivery();

        if (del == null) {
            return;
        }

        boolean owner = d.audience() == FinalReportAudience.OWNER;

        PdfPTable t = owner
                ? section("DELIVERY", new float[]{2f, 2f, 2f}, "Delivered on", "Delivered by", "Received by")
                : section("DELIVERY", new float[]{2f}, "Delivered on");

        t.addCell(bodyCell(dateTime(d, del.deliveredAt()), Element.ALIGN_LEFT));
        if (owner) {
            t.addCell(bodyCell(del.deliveredBy(), Element.ALIGN_LEFT));
            t.addCell(bodyCell(del.receivedBy(), Element.ALIGN_LEFT));
        }

        doc.add(t);
    }

    private void ownerWorkshop(Document doc, JobCardFinalReportDTO d) throws DocumentException {

        JobCardFinalReportDTO.OwnerInfo o = d.ownerInfo();

        if (o == null) {
            return;
        }

        List<TechnicianInfo> techs = o.technicians() == null ? List.of() : o.technicians();

        if (!techs.isEmpty()) {
            PdfPTable t = section("TECHNICIANS (INTERNAL)", new float[]{2.2f, 1.6f, 1.5f, 1f, 1.4f, 2f},
                    "Technician", "Type", "Status", "Est. h", "Actual time", "Remarks");
            for (TechnicianInfo x : techs) {
                t.addCell(bodyCell(x.name(), Element.ALIGN_LEFT));
                t.addCell(bodyCell(label(x.assignmentType()), Element.ALIGN_LEFT));
                t.addCell(bodyCell(label(x.status()), Element.ALIGN_LEFT));
                t.addCell(bodyCell(x.estimatedHours() == null ? "-" : String.valueOf(x.estimatedHours()), Element.ALIGN_RIGHT));
                t.addCell(bodyCell(com.garageos.core.util.ActualDuration.format(x.actualHours()), Element.ALIGN_RIGHT));
                t.addCell(bodyCell(x.remarks(), Element.ALIGN_LEFT));
            }
            doc.add(t);
        }

        if (!isBlank(o.jobCardRemarks())) {
            doc.add(textBox("JOB CARD REMARKS (INTERNAL)", o.jobCardRemarks()));
        }

        if (o.internalPhotoCount() > 0) {
            Paragraph p = new Paragraph(
                    o.internalPhotoCount() + " photo(s) in this report are internal and not shown to the customer.",
                    SMALL);
            p.setSpacingBefore(4);
            doc.add(p);
        }
    }

    private static int stageRank(String label) {
        if (label == null) return 9;
        String l = label.toLowerCase();
        if (l.startsWith("before")) return 0;
        if (l.startsWith("during")) return 1;
        if (l.startsWith("after")) return 2;
        return 3;
    }

    private void photos(Document doc, JobCardFinalReportDTO d) throws DocumentException {

        List<PhotoInfo> photos = d.photos();

        if (photos == null || photos.isEmpty()) {
            return;
        }

        doc.add(sectionHeader("PHOTOGRAPHS"));

        PdfPTable grid = new PdfPTable(2);
        grid.setWidthPercentage(100);

        // Grouped by stage in journey order (before, during, after), then anything else.
        java.util.Map<String, List<PhotoInfo>> byStage = new java.util.TreeMap<>(
                java.util.Comparator.comparingInt(FinalReportPdfRenderer::stageRank).thenComparing(x -> x));
        for (PhotoInfo p : photos) {
            byStage.computeIfAbsent(nullSafe(p.stageLabel(), "Photo"), k -> new java.util.ArrayList<>()).add(p);
        }

        for (java.util.Map.Entry<String, List<PhotoInfo>> group : byStage.entrySet()) {

            PdfPCell heading = new PdfPCell(new Paragraph(group.getKey().toUpperCase(), BOLD));
            heading.setColspan(2);
            heading.setBorder(Rectangle.NO_BORDER);
            heading.setPaddingTop(6);
            heading.setPaddingBottom(2);
            grid.addCell(heading);

            int placed = 0;

            for (PhotoInfo p : group.getValue()) {

                PdfPCell cell = new PdfPCell();
                cell.setBorderColor(BORDER);
                cell.setPadding(4);

                boolean drawn = false;

                if (p.jpegBytes() != null) {
                    try {
                        Image img = Image.getInstance(p.jpegBytes());
                        img.scaleToFit(240, 170);
                        img.setAlignment(Element.ALIGN_CENTER);
                        cell.addElement(img);
                        drawn = true;
                    } catch (IOException | RuntimeException e) {
                        drawn = false;
                    }
                }

                if (!drawn) {
                    Paragraph missing = new Paragraph("Photo not available in this report", SMALL);
                    missing.setAlignment(Element.ALIGN_CENTER);
                    missing.setSpacingBefore(60);
                    missing.setSpacingAfter(60);
                    cell.addElement(missing);
                }

                // Owner report only (a customer report never contains an internal photo): say so on the photo.
                if (d.audience() == com.garageos.core.enums.report.FinalReportAudience.OWNER && !p.customerVisible()) {
                    Paragraph tag = new Paragraph("INTERNAL - not shown to the customer", SMALL);
                    tag.setAlignment(Element.ALIGN_CENTER);
                    cell.addElement(tag);
                }

                grid.addCell(cell);
                placed++;
            }

            if (placed % 2 == 1) {
                grid.addCell(noBorder());
            }
        }

        doc.add(grid);
    }

    /**
     * Pickup and delivery evidence captured by drivers. Only recorded facts are printed: a missing capture time,
     * driver, GPS fix or address is shown as "not recorded", never guessed.
     */
    private void tripEvidence(Document doc, JobCardFinalReportDTO d) throws DocumentException {

        List<JobCardFinalReportDTO.TripEvidenceInfo> items = d.tripEvidence();

        if (items == null || items.isEmpty()) {
            return;
        }

        ZoneId zone = DisplayTime.resolveZone(d.garage() == null ? null : d.garage().getTimeZone());

        doc.add(sectionHeader("PICKUP AND DELIVERY EVIDENCE"));

        for (JobCardFinalReportDTO.TripEvidenceInfo e : items) {

            PdfPTable row = new PdfPTable(new float[] {1.1f, 1.9f});
            row.setWidthPercentage(100);
            row.setSpacingAfter(4);
            row.setKeepTogether(true);

            PdfPCell imageCell = new PdfPCell();
            imageCell.setBorderColor(BORDER);
            imageCell.setPadding(4);

            if (e.jpegBytes() != null) {
                try {
                    Image img = Image.getInstance(e.jpegBytes());
                    img.scaleToFit(150, 110);
                    imageCell.addElement(img);
                } catch (IOException | RuntimeException ex) {
                    imageCell.addElement(new Paragraph("Photo not available", SMALL));
                }
            } else {
                imageCell.addElement(new Paragraph(e.video() ? "Video (not embedded)" : "Photo not available", SMALL));
            }
            row.addCell(imageCell);

            PdfPCell textCell = new PdfPCell();
            textCell.setBorderColor(BORDER);
            textCell.setPadding(4);

            textCell.addElement(new Paragraph(
                    e.stageLabel() + (e.shotType() == null ? "" : " - " + label(e.shotType())), TABLE_HEAD));
            textCell.addElement(new Paragraph("Captured: " + stamp(e.capturedAt(), zone)
                    + ("SERVER".equals(e.capturedAtSource()) ? " (upload time; device time unavailable)" : ""), SMALL));
            if (e.uploadedAt() != null && !e.uploadedAt().equals(e.capturedAt())) {
                textCell.addElement(new Paragraph("Uploaded: " + stamp(e.uploadedAt(), zone), SMALL));
            }
            textCell.addElement(new Paragraph("By: " + (isBlank(e.capturedByName()) ? "not recorded" : e.capturedByName()), SMALL));
            textCell.addElement(new Paragraph("Location: " + location(e), SMALL));
            textCell.addElement(new Paragraph("Reference: EV-" + e.mediaId(), SMALL));
            row.addCell(textCell);

            doc.add(row);
        }
    }

    private static String stamp(LocalDateTime t, ZoneId zone) {
        return t == null ? "not recorded" : DisplayTime.format(t, ZoneId.systemDefault(), zone);
    }

    private static String location(JobCardFinalReportDTO.TripEvidenceInfo e) {
        StringBuilder sb = new StringBuilder();
        if (!isBlank(e.locationAddress())) {
            sb.append(e.locationAddress());
        }
        if (e.latitude() != null && e.longitude() != null) {
            if (sb.length() > 0) {
                sb.append("  ");
            }
            sb.append(String.format(java.util.Locale.ENGLISH, "%.5f, %.5f", e.latitude(), e.longitude()));
            if (e.accuracyMeters() != null) {
                sb.append(String.format(java.util.Locale.ENGLISH, " (+/-%d m)", Math.round(e.accuracyMeters())));
            }
        }
        return sb.length() == 0 ? "not recorded" : sb.toString();
    }

    private PdfPTable disclaimer(JobCardFinalReportDTO d) {

        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(10);

        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        cell.addElement(new Paragraph("Notes", TABLE_HEAD));
        cell.addElement(new Paragraph(
                d.audience() == FinalReportAudience.OWNER ? OWNER_DISCLAIMER : CUSTOMER_DISCLAIMER, SMALL));
        cell.addElement(new Paragraph(
                "Generated on " + dateTime(d, d.generatedAt()) + "  |  " + d.reference()
                        + "  |  Version " + d.version(), SMALL));

        t.addCell(cell);
        return t;
    }

    // ----------------------------------------------------------------- helpers

    /** A banner row plus a header row, repeated on page breaks, then the body rows the caller adds. */
    private PdfPTable section(String title, float[] widths, String... headers) {

        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        t.setHeaderRows(2);
        t.setSpacingBefore(6);
        t.setSplitLate(false);

        PdfPCell banner = new PdfPCell(new Phrase(title, SECTION));
        banner.setColspan(widths.length);
        banner.setBackgroundColor(GREEN);
        banner.setPadding(4);
        banner.setBorder(Rectangle.NO_BORDER);
        t.addCell(banner);

        for (int i = 0; i < headers.length; i++) {
            t.addCell(headCell(headers[i], i == 0 ? Element.ALIGN_LEFT
                    : (headers[i].equals("Qty") || headers[i].equals("Amount")
                    || headers[i].endsWith(" h") ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT)));
        }

        return t;
    }

    private PdfPTable textBox(String title, String text) {

        PdfPTable t = section(title, new float[]{1f}, "Details");
        t.addCell(bodyCell(text, Element.ALIGN_LEFT));
        return t;
    }

    private static String findingLabel(String status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case "PASS" -> "OK";
            case "FAIL" -> "Needs attention";
            case "REPAIR_REQUIRED" -> "Repair required";
            default -> label(status);
        };
    }

    private static String label(String enumName) {
        if (enumName == null || enumName.isBlank()) {
            return "-";
        }
        String s = enumName.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String date(LocalDate d) {
        return d == null ? "-" : d.format(DATE);
    }

    /**
     * Event times are stored as server-zone LocalDateTime values; show them in the garage timezone (documented
     * fallback Asia/Kolkata) with the zone abbreviation so the report is unambiguous.
     */
    private static String dateTime(JobCardFinalReportDTO report, LocalDateTime t) {
        String garageZone = report.garage() == null ? null : report.garage().getTimeZone();
        return DisplayTime.format(t, ZoneId.systemDefault(), DisplayTime.resolveZone(garageZone));
    }
}
