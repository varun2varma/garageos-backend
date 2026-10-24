package com.garageos.modules.finalreport.service;

import com.garageos.core.enums.InspectionFindingStatus;
import com.garageos.core.enums.report.FinalReportAudience;
import com.garageos.core.util.InvoiceTaxCalculator;
import com.garageos.modules.complaint.entity.Complaint;
import com.garageos.modules.complaint.repository.ComplaintRepository;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.delivery.entity.Delivery;
import com.garageos.modules.delivery.repository.DeliveryRepository;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.estimate.repository.EstimateRepository;
import com.garageos.modules.finalreport.dto.response.FinalReportProjection;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO.DeliveryInfo;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO.FindingInfo;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO.InspectionInfo;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO.InvoiceInfo;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO.JobInfo;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO.OwnerInfo;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO.PartyInfo;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO.PhotoInfo;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO.QualityInfo;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO.RepairInfo;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO.TechnicianInfo;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO.VehicleInfo;
import com.garageos.modules.garage.service.GarageBrandingService;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.inspection.entity.Inspection;
import com.garageos.modules.inspection.repository.InspectionRepository;
import com.garageos.modules.inspectionfinding.entity.InspectionFinding;
import com.garageos.modules.inspectionfinding.repository.InspectionFindingRepository;
import com.garageos.modules.invoice.entity.Invoice;
import com.garageos.modules.invoice.repository.InvoiceRepository;
import com.garageos.modules.invoice.service.InvoicePdfService;
import com.garageos.modules.jobassignment.entity.JobAssignment;
import com.garageos.modules.jobassignment.repository.JobAssignmentRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.media.entity.JobCardMedia;
import com.garageos.modules.media.repository.JobCardMediaRepository;
import com.garageos.modules.media.service.MediaContent;
import com.garageos.modules.media.service.MediaService;
import com.garageos.modules.qualitycheck.entity.QualityCheck;
import com.garageos.modules.qualitycheck.repository.QualityCheckRepository;
import com.garageos.modules.repairtask.entity.RepairTask;
import com.garageos.modules.repairtask.repository.RepairTaskRepository;
import com.garageos.modules.vehicle.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builds the {@link JobCardFinalReportDTO} for one job card from GarageST's own
 * records. This is the only place report data is gathered - the OWNER and
 * CUSTOMER reports differ only in what {@link FinalReportProjection} keeps and
 * in which media are loaded.
 *
 * Photo visibility is enforced here, server-side, at the query: a CUSTOMER
 * report reads only {@code CUSTOMER_VISIBLE} media rows, so a restricted photo's
 * bytes are never fetched, let alone rendered.
 *
 * Callers authorize the job card first; this class performs no authorization.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinalReportAssembler {

    static final String VISIBILITY_CUSTOMER_VISIBLE = "CUSTOMER_VISIBLE";

    /** Bound on photos per report so one report cannot grow without limit. */
    static final int MAX_PHOTOS = 60;

    static final String ADDITIONAL_PHOTOS_LABEL = "Additional photos (not shown in this report)";

    private static final Set<String> USABLE_UPLOAD_STATES = Set.of("COMPLETED", "UPLOADED", "PROCESSING");

    /** Uploads that are expected to finish: shown as a placeholder (not silently omitted) until they do. */
    private static final Set<String> IN_FLIGHT_UPLOAD_STATES =
            Set.of("PENDING", "UPLOADING", "RETRY_WAIT", "AUTH_REQUIRED");

    private final ComplaintRepository complaintRepository;
    private final InspectionRepository inspectionRepository;
    private final InspectionFindingRepository inspectionFindingRepository;
    private final EstimateRepository estimateRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoicePdfService invoicePdfService;
    private final RepairTaskRepository repairTaskRepository;
    private final QualityCheckRepository qualityCheckRepository;
    private final DeliveryRepository deliveryRepository;
    private final JobAssignmentRepository jobAssignmentRepository;
    private final JobCardMediaRepository jobCardMediaRepository;
    private final MediaService mediaService;
    private final GarageBrandingService garageBrandingService;
    private final FinalReportTripEvidenceLoader tripEvidenceLoader;

    /** Optional so hand-built unit-test instances keep working; absent = no declined-complaint information. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.garageos.modules.estimateitem.repository.EstimateItemRepository estimateItemRepository;

    /**
     * Complaints the customer removed from the visit: cancelled, or having estimate lines of which NONE is still
     * selected. Derived from persisted records only; a complaint with no estimate line at all is simply
     * unquoted, not "declined".
     */
    private List<String> declinedComplaints(Long jobCardId) {

        if (estimateItemRepository == null) {
            return List.of();
        }

        Estimate estimate = estimateRepository.findByJobCardId(jobCardId).orElse(null);

        java.util.Map<Long, List<com.garageos.modules.estimateitem.entity.EstimateItem>> byComplaint =
                new java.util.HashMap<>();

        if (estimate != null) {
            for (com.garageos.modules.estimateitem.entity.EstimateItem item
                    : estimateItemRepository.findByEstimateId(estimate.getId())) {
                if (item.getComplaint() != null) {
                    byComplaint.computeIfAbsent(item.getComplaint().getId(), k -> new ArrayList<>()).add(item);
                }
            }
        }

        List<String> declined = new ArrayList<>();

        for (Complaint c : complaintRepository.findByJobCardId(jobCardId)) {
            List<com.garageos.modules.estimateitem.entity.EstimateItem> lines = byComplaint.get(c.getId());
            boolean cancelled = c.getStatus() == com.garageos.core.enums.ComplaintStatus.CANCELLED;
            boolean allDeselected = lines != null && !lines.isEmpty()
                    && lines.stream().noneMatch(l -> Boolean.TRUE.equals(l.getSelected()));

            if (cancelled || allDeselected) {
                declined.add(c.getComplaint());
            }
        }

        return declined;
    }

    public JobCardFinalReportDTO assemble(
            JobCard jobCard, FinalReportAudience audience, int version, LocalDateTime generatedAt) {

        Long jobCardId = jobCard.getId();
        Long garageId = jobCard.getGarage().getId();
        Customer customer = jobCard.getCustomer();
        Vehicle vehicle = jobCard.getVehicle();

        GarageBrandingService.LogoContent logo = garageBrandingService.loadLogoOrNull(garageId);

        Estimate estimate = estimateRepository.findByJobCardId(jobCardId).orElse(null);
        Invoice invoice = invoiceRepository.findByEstimateJobCardId(jobCardId).orElse(null);

        InvoiceTaxCalculator.Breakdown work =
                invoice == null ? null : invoicePdfService.breakdownFor(invoice);

        Delivery delivery = deliveryRepository.findByJobCardId(jobCardId).orElse(null);
        QualityCheck quality = qualityCheckRepository.findByJobCardId(jobCardId).orElse(null);

        List<PhotoInfo> photos = loadPhotos(jobCardId, audience);

        JobCardFinalReportDTO full = new JobCardFinalReportDTO(
                audience,
                version,
                reference(jobCard, audience, version),
                generatedAt,
                garageBrandingService.resolve(garageId),
                logo == null ? null : logo.bytes(),
                new PartyInfo(customer.getFullName(), customer.getMobileNumber(), customerAddress(customer)),
                new VehicleInfo(
                        vehicle.getRegistrationNumber(),
                        join(" ", vehicle.getBrand(), vehicle.getModel(), vehicle.getVariant()),
                        join("  |  ",
                                vehicle.getFuelType() == null ? null : vehicle.getFuelType().name(),
                                vehicle.getManufacturingYear() == null ? null : "Year " + vehicle.getManufacturingYear(),
                                vehicle.getColor())),
                new JobInfo(
                        jobCard.getJobCardNumber(),
                        jobCard.getServiceDate(),
                        jobCard.getOdometerReading(),
                        jobCard.getStatus() == null ? null : jobCard.getStatus().name(),
                        jobCard.getEstimatedDeliveryDate(),
                        com.garageos.core.util.StaffAttribution.serviceAdvisorLabel(jobCard)),
                complaintRepository.findByJobCardId(jobCardId).stream().map(Complaint::getComplaint).toList(),
                inspection(jobCardId),
                estimate == null ? null : estimate.getEstimateNumber(),
                estimate == null || estimate.getStatus() == null ? null : estimate.getStatus().name(),
                work,
                repairs(jobCardId),
                quality == null ? null : new QualityInfo(
                        quality.getStatus() == null ? null : quality.getStatus().name(),
                        quality.getInspectedAt(), quality.getInspectedBy(), quality.getRemarks()),
                invoice == null ? null : new InvoiceInfo(
                        invoice.getInvoiceNumber(),
                        invoice.getGeneratedAt() == null ? null : invoice.getGeneratedAt().toLocalDate(),
                        work.grandTotal(),
                        invoice.getPaymentStatus() == null ? null : invoice.getPaymentStatus().name()),
                delivery == null ? null : new DeliveryInfo(
                        delivery.getDeliveryDateTime(), delivery.getDeliveredBy(), delivery.getReceivedBy()),
                photos,
                new OwnerInfo(
                        jobCard.getRemarks(),
                        technicians(jobCardId),
                        // Owner report only: how many of its photos are internal (never present for a customer).
                        audience == FinalReportAudience.OWNER
                                ? (int) photos.stream().filter(p -> !p.customerVisible()).count()
                                : 0),
                // Selected by relationship and filtered for the audience inside the loader (server side).
                tripEvidenceLoader.load(jobCard, audience),
                declinedComplaints(jobCardId));

        return FinalReportProjection.project(full, audience);
    }

    // ---------------------------------------------------------------- sections

    private InspectionInfo inspection(Long jobCardId) {

        List<Inspection> inspections = inspectionRepository.findByComplaintJobCardId(jobCardId);

        List<FindingInfo> findings = new ArrayList<>();

        for (InspectionFinding f : inspectionFindingRepository.findByJobCardIdOrderById(jobCardId)) {
            if (f.getStatus() == null || f.getStatus() == InspectionFindingStatus.NOT_INSPECTED) {
                continue;
            }
            findings.add(new FindingInfo(
                    f.getInspectionMasterItem().getCategory(),
                    f.getInspectionMasterItem().getCheckItem(),
                    f.getStatus().name(),
                    f.getRemarks()));
        }

        if (inspections.isEmpty() && findings.isEmpty()) {
            return null;
        }

        String notes = joinDistinct(inspections.stream().map(Inspection::getInspectionNotes).toList());
        String recommended = joinDistinct(inspections.stream().map(Inspection::getRecommendedWork).toList());

        return new InspectionInfo(notes, recommended, findings);
    }

    private List<RepairInfo> repairs(Long jobCardId) {

        List<RepairInfo> out = new ArrayList<>();

        for (RepairTask t : repairTaskRepository.findByJobCardIdOrderById(jobCardId)) {

            // A cancelled task (e.g. the customer removed the complaint after a task had been created) is not
            // work that was done: it never appears as repair work.
            if (t.getStatus() == com.garageos.core.enums.RepairStatus.CANCELLED) {
                continue;
            }

            // One repair task = one customer complaint, so the work is named by the complaint it resolves. (The
            // task's "representative" estimate line is just its first part/labour line and would mislabel it.)
            String description = t.getComplaint() != null
                    ? t.getComplaint().getComplaint()
                    : (t.getEstimateItem() != null ? t.getEstimateItem().getDescription() : "Repair task");

            out.add(new RepairInfo(
                    description,
                    t.getStatus() == null ? null : t.getStatus().name(),
                    t.getCompletedAt(),
                    com.garageos.core.util.ActualDuration.hours(t.getStartedAt(), t.getCompletedAt()),
                    t.getTechnicianName(),
                    t.getRemarks()));
        }

        return out;
    }

    private List<TechnicianInfo> technicians(Long jobCardId) {

        List<TechnicianInfo> out = new ArrayList<>();

        for (JobAssignment a : jobAssignmentRepository.findByJobCardId(jobCardId)) {
            User u = a.getUser();
            out.add(new TechnicianInfo(
                    u == null ? "-" : join(" ", u.getFirstName(), u.getLastName()),
                    a.getAssignmentType() == null ? null : a.getAssignmentType().name(),
                    a.getStatus() == null ? null : a.getStatus().name(),
                    a.getEstimatedHours(),
                    // Actual = the RepairTask's recorded start -> completion; never the typed or
                    // estimated hours, and never the assignment's possibly stale copy of the timestamps.
                    a.getRepairTask() == null ? null : com.garageos.core.util.ActualDuration.hours(
                            a.getRepairTask().getStartedAt(), a.getRepairTask().getCompletedAt()),
                    a.getCompletedAt(),
                    a.getRemarks()));
        }

        return out;
    }

    /**
     * The visibility rule lives in this query choice, on the server, before any object is read from storage:
     * <ul>
     *   <li>CUSTOMER report: only CUSTOMER_VISIBLE rows. Internal media is never read for a customer.</li>
     *   <li>OWNER report: the garage's complete evidence, internal and customer-visible, each photo carrying its
     *       stored visibility so the PDF can mark the internal ones.</li>
     * </ul>
     * The CUSTOMER projection filters again ({@code FinalReportProjection}) as a second line of defence.
     */
    private List<PhotoInfo> loadPhotos(Long jobCardId, FinalReportAudience audience) {

        List<JobCardMedia> rows = audience == FinalReportAudience.OWNER
                ? jobCardMediaRepository.findByJobCardIdOrderByCreatedAtAsc(jobCardId)
                : jobCardMediaRepository.findByJobCardIdAndVisibilityOrderByCreatedAtAsc(
                        jobCardId, VISIBILITY_CUSTOMER_VISIBLE);

        List<PhotoInfo> photos = new ArrayList<>();

        for (JobCardMedia media : rows) {

            if (photos.size() >= MAX_PHOTOS) {
                // Never a silent drop: one visible placeholder says more photos exist than the report can hold.
                long remaining = rows.stream().filter(r -> "IMAGE".equals(r.getMediaType())).count() - photos.size();
                log.warn("[FINAL-REPORT] Photo cap reached. jobCardId={}, shown={}, notShown={}", jobCardId, photos.size(), remaining);
                photos.add(new PhotoInfo(null, ADDITIONAL_PHOTOS_LABEL, true, null));
                break;
            }

            if (!"IMAGE".equals(media.getMediaType())) {
                continue;
            }

            boolean visible = VISIBILITY_CUSTOMER_VISIBLE.equals(media.getVisibility());

            if (!USABLE_UPLOAD_STATES.contains(media.getUploadStatus())) {
                // In flight, or failed/cancelled in the canonical sequence: keep its slot as a visible "Photo not
                // available" placeholder. A failed row WITHOUT a media sequence is a superseded retry (the canonical
                // row of that sequence is reported instead), so it is skipped - it is not a missing photo.
                boolean inFlight = IN_FLIGHT_UPLOAD_STATES.contains(media.getUploadStatus());
                if (inFlight || media.getMediaSequence() != null) {
                    if (!inFlight) {
                        log.warn("[FINAL-REPORT] Photo upload not usable. jobCardId={}, mediaId={}, status={}",
                                jobCardId, media.getId(), media.getUploadStatus());
                    }
                    photos.add(new PhotoInfo(media.getId(), stageLabel(media.getMediaStage()), visible, null));
                }
                continue;
            }

            byte[] jpeg = null;

            // One retry: a transient storage hiccup must not drop an expected photo from a closed job's report.
            for (int attempt = 1; attempt <= 2 && jpeg == null; attempt++) {
                try {
                    MediaContent content = mediaService.downloadContent(media);
                    jpeg = ReportImageScaler.toReportJpeg(content.content());
                } catch (RuntimeException e) {
                    log.warn("[FINAL-REPORT] Photo unreadable. jobCardId={}, mediaId={}, attempt={}, reason={}",
                            jobCardId, media.getId(), attempt, e.getClass().getSimpleName());
                }
            }

            // Unreadable after retry: still listed (placeholder) so the gap is visible, never a failed report.
            photos.add(new PhotoInfo(media.getId(), stageLabel(media.getMediaStage()), visible, jpeg));
        }

        return photos;
    }

    // ----------------------------------------------------------------- helpers

    static String reference(JobCard jobCard, FinalReportAudience audience, int version) {
        return "GST-FR-" + jobCard.getJobCardNumber()
                + (audience == FinalReportAudience.OWNER ? "-O" : "-C") + "-v" + version;
    }

    private static String stageLabel(String stage) {
        if (stage == null) {
            return "Photo";
        }
        return switch (stage) {
            case "BEFORE_SERVICE" -> "Before service";
            case "DURING_REPAIR" -> "During repair";
            case "AFTER_REPAIR" -> "After repair";
            default -> "Photo";
        };
    }

    private static String customerAddress(Customer customer) {

        String cityLine = join(", ", customer.getCity(), customer.getState());

        if (customer.getPincode() != null && !customer.getPincode().isBlank()) {
            cityLine = cityLine + " - " + customer.getPincode();
        }

        return join(", ", customer.getAddress(), cityLine);
    }

    private static String joinDistinct(List<String> values) {
        List<String> distinct = new ArrayList<>();
        for (String v : values) {
            if (v != null && !v.isBlank() && !distinct.contains(v.trim())) {
                distinct.add(v.trim());
            }
        }
        return distinct.isEmpty() ? null : String.join("\n", distinct);
    }

    private static String join(String separator, String... parts) {

        List<String> present = new ArrayList<>();

        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                present.add(part.trim());
            }
        }

        return String.join(separator, present);
    }
}
