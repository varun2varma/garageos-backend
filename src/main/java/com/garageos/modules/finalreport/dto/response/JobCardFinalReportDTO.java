package com.garageos.modules.finalreport.dto.response;

import com.garageos.core.enums.report.FinalReportAudience;
import com.garageos.core.util.InvoiceTaxCalculator;
import com.garageos.modules.garage.dto.response.GarageBrandingResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The single data model behind both final reports. It is assembled once from
 * GarageST's own records (nothing is copied into a second source of truth) and
 * then narrowed per audience by {@link FinalReportProjection}. Fields that only
 * the owner may see are null / empty in the CUSTOMER projection; sections with
 * no data are null / empty so the renderer simply omits them.
 */
public record JobCardFinalReportDTO(
        FinalReportAudience audience,
        int version,
        String reference,
        LocalDateTime generatedAt,
        GarageBrandingResponse garage,
        byte[] logoBytes,
        PartyInfo customer,
        VehicleInfo vehicle,
        JobInfo job,
        List<String> complaints,
        InspectionInfo inspection,
        String estimateNumber,
        String estimateStatus,
        /** Approved / billed work, exactly as invoiced (parts, labour, other charges). */
        InvoiceTaxCalculator.Breakdown work,
        List<RepairInfo> repairs,
        QualityInfo qualityCheck,
        InvoiceInfo invoice,
        DeliveryInfo delivery,
        List<PhotoInfo> photos,
        /** OWNER audience only; null for CUSTOMER. */
        OwnerInfo ownerInfo,
        /** Driver pickup/delivery evidence linked to this job card, already filtered for the audience. */
        List<TripEvidenceInfo> tripEvidence) {

    /** Compatibility constructor for callers that carry no trip evidence. */
    public JobCardFinalReportDTO(
            FinalReportAudience audience, int version, String reference, LocalDateTime generatedAt,
            GarageBrandingResponse garage, byte[] logoBytes, PartyInfo customer, VehicleInfo vehicle, JobInfo job,
            List<String> complaints, InspectionInfo inspection, String estimateNumber, String estimateStatus,
            InvoiceTaxCalculator.Breakdown work, List<RepairInfo> repairs, QualityInfo qualityCheck,
            InvoiceInfo invoice, DeliveryInfo delivery, List<PhotoInfo> photos, OwnerInfo ownerInfo) {
        this(audience, version, reference, generatedAt, garage, logoBytes, customer, vehicle, job, complaints,
                inspection, estimateNumber, estimateStatus, work, repairs, qualityCheck, invoice, delivery, photos,
                ownerInfo, List.of());
    }

    /**
     * One driver-captured pickup/delivery item. Every metadata field is null when it was not recorded - nothing is
     * invented. {@code jpegBytes} is null for videos and for photos whose object could not be read.
     */
    public record TripEvidenceInfo(
            Long mediaId,
            String stageLabel,
            String shotType,
            boolean video,
            LocalDateTime capturedAt,
            String capturedAtSource,
            LocalDateTime uploadedAt,
            String capturedByName,
            Double latitude,
            Double longitude,
            Double accuracyMeters,
            String locationAddress,
            byte[] jpegBytes) {
    }

    public record PartyInfo(String name, String mobile, String address) {
    }

    public record VehicleInfo(String registrationNumber, String description, String details) {
    }

    public record JobInfo(
            String jobCardNumber,
            LocalDate serviceDate,
            Long odometerIn,
            String status,
            LocalDate estimatedDeliveryDate) {
    }

    public record InspectionInfo(
            /** Inspector's notes - owner only. */
            String notes,
            String recommendedWork,
            List<FindingInfo> findings) {
    }

    public record FindingInfo(
            String category,
            String item,
            String status,
            /** Finding remarks - owner only. */
            String remarks) {
    }

    public record RepairInfo(
            String description,
            String status,
            LocalDateTime completedAt,
            /** Owner only. */
            String technicianName,
            /** Owner only. */
            String remarks) {
    }

    public record QualityInfo(
            String status,
            LocalDateTime inspectedAt,
            /** Owner only. */
            String inspectedBy,
            /** Owner only. */
            String remarks) {
    }

    public record InvoiceInfo(
            String invoiceNumber,
            LocalDate invoiceDate,
            BigDecimal grandTotal,
            String paymentStatus) {
    }

    public record DeliveryInfo(
            LocalDateTime deliveredAt,
            /** Owner only. */
            String deliveredBy,
            /** Owner only. */
            String receivedBy) {
    }

    /**
     * A photo that made it into the report. {@code customerVisible} mirrors the
     * stored media visibility so the CUSTOMER projection can drop anything that
     * is not CUSTOMER_VISIBLE even if it was loaded by mistake.
     */
    public record PhotoInfo(
            Long mediaId,
            String stageLabel,
            boolean customerVisible,
            byte[] jpegBytes) {
    }

    public record OwnerInfo(
            String jobCardRemarks,
            List<TechnicianInfo> technicians,
            /** Number of photos kept internal (not shown to the customer). */
            int internalPhotoCount) {
    }

    public record TechnicianInfo(
            String name,
            String assignmentType,
            String status,
            Double estimatedHours,
            Double actualHours,
            LocalDateTime completedAt,
            String remarks) {
    }
}
