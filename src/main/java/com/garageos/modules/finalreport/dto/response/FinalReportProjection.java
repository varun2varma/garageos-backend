package com.garageos.modules.finalreport.dto.response;

import com.garageos.core.enums.report.FinalReportAudience;

import java.util.List;

/**
 * Narrows the shared {@link JobCardFinalReportDTO} to what one audience may
 * see. The CUSTOMER projection removes every owner-only field and drops any photo
 * that is not customer-visible. The OWNER projection keeps the garage's complete
 * evidence set (internal and customer-visible photos).
 *
 * This is the second line of defence for the customer: the assembler already loads
 * only customer-visible media for a customer report, so restricted photos are never
 * even read from storage.
 */
public final class FinalReportProjection {

    private FinalReportProjection() {
    }

    public static JobCardFinalReportDTO project(JobCardFinalReportDTO full, FinalReportAudience audience) {
        return audience == FinalReportAudience.OWNER ? owner(full) : customer(full);
    }

    public static JobCardFinalReportDTO owner(JobCardFinalReportDTO f) {
        return copy(f, FinalReportAudience.OWNER, f.inspection(), f.repairs(), f.qualityCheck(),
                f.delivery(), f.photos() == null ? List.of() : f.photos(), f.ownerInfo());
    }

    public static JobCardFinalReportDTO customer(JobCardFinalReportDTO f) {

        JobCardFinalReportDTO.InspectionInfo inspection = f.inspection() == null ? null
                : new JobCardFinalReportDTO.InspectionInfo(
                        null,
                        f.inspection().recommendedWork(),
                        f.inspection().findings() == null ? List.of()
                                : f.inspection().findings().stream()
                                .map(x -> new JobCardFinalReportDTO.FindingInfo(
                                        x.category(), x.item(), x.status(), null))
                                .toList());

        List<JobCardFinalReportDTO.RepairInfo> repairs = f.repairs() == null ? List.of()
                : f.repairs().stream()
                .map(r -> new JobCardFinalReportDTO.RepairInfo(
                        r.description(), r.status(), r.completedAt(), r.actualHours(), null, null))
                .toList();

        JobCardFinalReportDTO.QualityInfo quality = f.qualityCheck() == null ? null
                : new JobCardFinalReportDTO.QualityInfo(
                        f.qualityCheck().status(), f.qualityCheck().inspectedAt(), null, null);

        JobCardFinalReportDTO.DeliveryInfo delivery = f.delivery() == null ? null
                : new JobCardFinalReportDTO.DeliveryInfo(f.delivery().deliveredAt(), null, null);

        return copy(f, FinalReportAudience.CUSTOMER, inspection, repairs, quality, delivery,
                customerVisiblePhotos(f), null);
    }

    private static List<JobCardFinalReportDTO.PhotoInfo> customerVisiblePhotos(JobCardFinalReportDTO f) {
        return f.photos() == null ? List.of()
                : f.photos().stream().filter(JobCardFinalReportDTO.PhotoInfo::customerVisible).toList();
    }

    private static JobCardFinalReportDTO copy(
            JobCardFinalReportDTO f,
            FinalReportAudience audience,
            JobCardFinalReportDTO.InspectionInfo inspection,
            List<JobCardFinalReportDTO.RepairInfo> repairs,
            JobCardFinalReportDTO.QualityInfo quality,
            JobCardFinalReportDTO.DeliveryInfo delivery,
            List<JobCardFinalReportDTO.PhotoInfo> photos,
            JobCardFinalReportDTO.OwnerInfo ownerInfo) {

        return new JobCardFinalReportDTO(
                audience, f.version(), f.reference(), f.generatedAt(), f.garage(), f.logoBytes(),
                f.customer(), f.vehicle(), f.job(), f.complaints(), inspection,
                f.estimateNumber(), f.estimateStatus(), f.work(), repairs, quality,
                f.invoice(), delivery, photos, ownerInfo, f.tripEvidence(), f.declinedComplaints());
    }
}
