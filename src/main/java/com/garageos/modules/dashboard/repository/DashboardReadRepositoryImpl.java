package com.garageos.modules.dashboard.repository;

import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.QualityCheckStatus;
import com.garageos.modules.dashboard.dto.response.DashboardSummaryResponse;
import com.garageos.modules.dashboard.dto.response.RecentJobResponse;
import com.garageos.modules.jobcard.entity.JobCard;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Corrective fix: every query here previously aggregated across ALL
 * garages with no tenant scoping whatsoever - every Manager/Advisor/Owner
 * dashboard, regardless of garage, showed the exact same system-wide
 * numbers and the same "recent jobs" list. All counts are now scoped to
 * [garageIds] via JobCard.garage - the actual garage-scoped aggregate root
 * (Customer/Vehicle are not garage-owned entities in this data model, so
 * "customers"/"vehicles" here means "distinct customers/vehicles with at
 * least one Job Card at one of these garages", not a global count).
 *
 * [garageIds] is a list (not a single id) so a multi-garage Owner's
 * "All Garages" view can aggregate across every garage they belong to in
 * one query, using the same collection for a single-garage caller
 * (a one-element list) - no separate code path needed.
 */
@Repository
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardReadRepositoryImpl implements DashboardReadRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public DashboardSummaryResponse getDashboardSummary(List<Long> garageIds) {

        Long activeJobs = entityManager.createQuery("""
                SELECT COUNT(j)
                FROM JobCard j
                WHERE j.garage.id IN :garageIds
                AND j.status NOT IN (:closed, :cancelled)
                """, Long.class)
                .setParameter("garageIds", garageIds)
                .setParameter("closed", JobCardStatus.CLOSED)
                .setParameter("cancelled", JobCardStatus.CANCELLED)
                .getSingleResult();

        Long pendingEstimates = entityManager.createQuery("""
                SELECT COUNT(e)
                FROM Estimate e
                WHERE e.jobCard.garage.id IN :garageIds
                AND e.status = :status
                """, Long.class)
                .setParameter("garageIds", garageIds)
                .setParameter("status", EstimateStatus.WAITING_FOR_APPROVAL)
                .getSingleResult();

        Long readyForDelivery = countByStatus(garageIds, JobCardStatus.READY_FOR_DELIVERY);

        // Corrective fix: previously filtered on j.serviceDate = :today, which
        // is set once at job creation and never updated - this counted jobs
        // OPENED today, not jobs actually completed today. DELIVERED/CLOSED
        // are the only two terminal statuses a JobCard reaches, and
        // BaseEntity.updatedAt (@LastModifiedDate) reflects the moment that
        // status transition was persisted, so filtering on updatedAt against
        // a terminal status is the real "completed today" signal.
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        Long completedToday = entityManager.createQuery("""
                SELECT COUNT(j)
                FROM JobCard j
                WHERE j.garage.id IN :garageIds
                AND j.status IN (:delivered, :closed)
                AND j.updatedAt >= :todayStart
                AND j.updatedAt < :todayEnd
                """, Long.class)
                .setParameter("garageIds", garageIds)
                .setParameter("delivered", JobCardStatus.DELIVERED)
                .setParameter("closed", JobCardStatus.CLOSED)
                .setParameter("todayStart", todayStart)
                .setParameter("todayEnd", todayEnd)
                .getSingleResult();

        BigDecimal todayRevenue = entityManager.createQuery("""
                SELECT COALESCE(SUM(i.grandTotal),0)
                FROM Invoice i
                WHERE i.estimate.jobCard.garage.id IN :garageIds
                AND i.generatedAt >= :today
                """, BigDecimal.class)
                .setParameter("garageIds", garageIds)
                .setParameter("today", LocalDate.now().atStartOfDay())
                .getSingleResult();

        Long inspectionJobs = countByStatus(garageIds, JobCardStatus.INSPECTION_PENDING);
        Long estimateJobs = countByStatus(garageIds, JobCardStatus.ESTIMATE_PENDING);
        // Corrective fix: REPAIR_IN_PROGRESS is only reached via the legacy
        // JobCardServiceImpl.startRepair() endpoint. The canonical path
        // (EstimateServiceImpl.approveEstimateCanonical -> RepairTaskService)
        // sets REPAIR_PENDING instead, so counting only REPAIR_IN_PROGRESS
        // undercounts (often to 0) jobs actually in the repair phase.
        Long repairJobs = countByStatusIn(garageIds, JobCardStatus.REPAIR_PENDING, JobCardStatus.REPAIR_IN_PROGRESS);
        // Corrective fix (round 2): JobCardStatus.QUALITY_CHECK is a dead
        // enum value (see below), and the first fix approximated "awaiting
        // QC" via JobCard.status == REPAIR_COMPLETED. That is a correct
        // proxy today (RepairTaskServiceImpl.completeRepair sets
        // REPAIR_COMPLETED and creates the QualityCheck row in the same
        // transaction, and a QC fail moves the JobCard back to
        // REPAIR_PENDING, not REPAIR_COMPLETED) but it is still a proxy
        // through JobCard state rather than the actual QC record. The
        // QualityCheck entity has its own authoritative status
        // (PENDING/PASSED/FAILED, one row per JobCard - QualityCheck.java),
        // so "genuinely awaiting QC" is expressed directly against it
        // instead of being inferred.
        Long qualityCheckJobs = entityManager.createQuery("""
                SELECT COUNT(q)
                FROM QualityCheck q
                WHERE q.jobCard.garage.id IN :garageIds
                AND q.status = :status
                """, Long.class)
                .setParameter("garageIds", garageIds)
                .setParameter("status", QualityCheckStatus.PENDING)
                .getSingleResult();
        Long invoiceJobs = countByStatus(garageIds, JobCardStatus.READY_FOR_INVOICE);
        // Corrective fix: JobCardStatus.PAYMENT_PENDING is a dead enum value -
        // InvoiceServiceImpl never writes it. INVOICE_GENERATED is the real
        // "invoice generated, payment not yet received" state (payment
        // receipt moves the JobCard straight to READY_FOR_DELIVERY).
        Long paymentPending = countByStatus(garageIds, JobCardStatus.INVOICE_GENERATED);

        Long totalCustomers = entityManager.createQuery("""
                SELECT COUNT(DISTINCT j.customer)
                FROM JobCard j
                WHERE j.garage.id IN :garageIds
                """, Long.class)
                .setParameter("garageIds", garageIds)
                .getSingleResult();

        Long totalVehicles = entityManager.createQuery("""
                SELECT COUNT(DISTINCT j.vehicle)
                FROM JobCard j
                WHERE j.garage.id IN :garageIds
                """, Long.class)
                .setParameter("garageIds", garageIds)
                .getSingleResult();

        Long totalJobCards = entityManager.createQuery("""
                SELECT COUNT(j)
                FROM JobCard j
                WHERE j.garage.id IN :garageIds
                """, Long.class)
                .setParameter("garageIds", garageIds)
                .getSingleResult();

        Long totalInvoices = entityManager.createQuery("""
                SELECT COUNT(i)
                FROM Invoice i
                WHERE i.estimate.jobCard.garage.id IN :garageIds
                """, Long.class)
                .setParameter("garageIds", garageIds)
                .getSingleResult();

        return DashboardSummaryResponse.builder()
                .totalCustomers(totalCustomers)
                .totalVehicles(totalVehicles)
                .totalJobCards(totalJobCards)
                .totalInvoices(totalInvoices)
                .activeJobs(activeJobs)
                .pendingEstimates(pendingEstimates)
                .readyForDelivery(readyForDelivery)
                .completedToday(completedToday)
                .todayRevenue(todayRevenue)
                .inspectionJobs(inspectionJobs)
                .estimateJobs(estimateJobs)
                .repairJobs(repairJobs)
                .qualityCheckJobs(qualityCheckJobs)
                .readyForInvoiceJobs(invoiceJobs)
                .paymentPendingJobs(paymentPending)
                .build();
    }

    private Long countByStatus(List<Long> garageIds, JobCardStatus status) {

        return entityManager.createQuery("""
                SELECT COUNT(j)
                FROM JobCard j
                WHERE j.garage.id IN :garageIds
                AND j.status = :status
                """, Long.class)
                .setParameter("garageIds", garageIds)
                .setParameter("status", status)
                .getSingleResult();
    }

    private Long countByStatusIn(List<Long> garageIds, JobCardStatus... statuses) {

        return entityManager.createQuery("""
                SELECT COUNT(j)
                FROM JobCard j
                WHERE j.garage.id IN :garageIds
                AND j.status IN :statuses
                """, Long.class)
                .setParameter("garageIds", garageIds)
                .setParameter("statuses", List.of(statuses))
                .getSingleResult();
    }

    @Override
    public List<RecentJobResponse> getRecentJobs(List<Long> garageIds) {

        List<JobCard> jobs = entityManager.createQuery("""
                SELECT j
                FROM JobCard j
                WHERE j.garage.id IN :garageIds
                ORDER BY j.createdAt DESC
                """, JobCard.class)
                .setParameter("garageIds", garageIds)
                .setMaxResults(10)
                .getResultList();

        return jobs.stream()
                .map(job -> RecentJobResponse.builder()
                        .jobCardId(job.getId())
                        .jobCardNumber(job.getJobCardNumber())
                        .customerName(job.getCustomer().getFullName())
                        .mobileNumber(job.getCustomer().getMobileNumber())
                        .registrationNumber(job.getVehicle().getRegistrationNumber())
                        .vehicleName(job.getVehicle().getBrand() + " " + job.getVehicle().getModel())
                        .status(job.getStatus())
                        .serviceDate(job.getServiceDate())
                        .odometerReading(job.getOdometerReading())
                        .estimatedDeliveryDate(job.getEstimatedDeliveryDate())
                        .build())
                .toList();
    }
}
