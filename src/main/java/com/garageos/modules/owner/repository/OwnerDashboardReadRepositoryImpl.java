package com.garageos.modules.owner.repository;

import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.QualityCheckStatus;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.owner.dto.response.OwnerDashboardRecentJobResponse;
import com.garageos.modules.owner.dto.response.OwnerDashboardSummaryResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OwnerDashboardReadRepositoryImpl
        implements OwnerDashboardReadRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public OwnerDashboardSummaryResponse getSummary(
            List<Long> garageIds,
            LocalDate from,
            LocalDate to
    ) {

        /*
         * ================================================================
         * SELECTED PERIOD
         * ================================================================
         */

        LocalDateTime rangeStart =
                from.atStartOfDay();

        LocalDateTime rangeEnd =
                to.plusDays(1).atStartOfDay();

        /*
         * Jobs CREATED during selected period.
         */
        Long jobsCreated =
                entityManager.createQuery("""
                        SELECT COUNT(j)
                        FROM JobCard j
                        WHERE j.garage.id IN :garageIds
                        AND j.createdAt >= :rangeStart
                        AND j.createdAt < :rangeEnd
                        """, Long.class)
                        .setParameter("garageIds", garageIds)
                        .setParameter("rangeStart", rangeStart)
                        .setParameter("rangeEnd", rangeEnd)
                        .getSingleResult();

        /*
         * Jobs COMPLETED during selected period.
         *
         * DELIVERED and CLOSED are the terminal job-card states.
         *
         * We use updatedAt because the current model does not expose
         * a dedicated completedAt field on JobCard.
         */
        Long completedJobs =
                entityManager.createQuery("""
                        SELECT COUNT(j)
                        FROM JobCard j
                        WHERE j.garage.id IN :garageIds
                        AND j.status IN (:delivered, :closed)
                        AND j.updatedAt >= :rangeStart
                        AND j.updatedAt < :rangeEnd
                        """, Long.class)
                        .setParameter("garageIds", garageIds)
                        .setParameter("delivered", JobCardStatus.DELIVERED)
                        .setParameter("closed", JobCardStatus.CLOSED)
                        .setParameter("rangeStart", rangeStart)
                        .setParameter("rangeEnd", rangeEnd)
                        .getSingleResult();

        /*
         * Revenue generated during selected period.
         */
        BigDecimal revenue =
                entityManager.createQuery("""
                        SELECT COALESCE(SUM(i.grandTotal), 0)
                        FROM Invoice i
                        WHERE i.estimate.jobCard.garage.id IN :garageIds
                        AND i.generatedAt >= :rangeStart
                        AND i.generatedAt < :rangeEnd
                        """, BigDecimal.class)
                        .setParameter("garageIds", garageIds)
                        .setParameter("rangeStart", rangeStart)
                        .setParameter("rangeEnd", rangeEnd)
                        .getSingleResult();

        /*
         * ================================================================
         * CURRENT OPERATIONAL SNAPSHOT
         * ================================================================
         *
         * These deliberately do NOT use from/to.
         *
         * Owner needs to know what is currently sitting in the pipeline.
         */

        Long activeJobs =
                entityManager.createQuery("""
                        SELECT COUNT(j)
                        FROM JobCard j
                        WHERE j.garage.id IN :garageIds
                        AND j.status NOT IN (:closed, :cancelled)
                        """, Long.class)
                        .setParameter("garageIds", garageIds)
                        .setParameter("closed", JobCardStatus.CLOSED)
                        .setParameter("cancelled", JobCardStatus.CANCELLED)
                        .getSingleResult();

        Long pendingEstimates =
                entityManager.createQuery("""
                        SELECT COUNT(e)
                        FROM Estimate e
                        WHERE e.jobCard.garage.id IN :garageIds
                        AND e.status = :status
                        """, Long.class)
                        .setParameter("garageIds", garageIds)
                        .setParameter(
                                "status",
                                EstimateStatus.WAITING_FOR_APPROVAL
                        )
                        .getSingleResult();

        Long readyForDelivery =
                countByStatus(
                        garageIds,
                        JobCardStatus.READY_FOR_DELIVERY
                );

        Long inspectionJobs =
                countByStatus(
                        garageIds,
                        JobCardStatus.INSPECTION_PENDING
                );

        Long estimateJobs =
                countByStatus(
                        garageIds,
                        JobCardStatus.ESTIMATE_PENDING
                );

        /*
         * Canonical repair flow can be:
         *
         * REPAIR_PENDING
         * REPAIR_IN_PROGRESS
         */
        Long repairJobs =
                countByStatusIn(
                        garageIds,
                        JobCardStatus.REPAIR_PENDING,
                        JobCardStatus.REPAIR_IN_PROGRESS
                );

        /*
         * QualityCheck has its own authoritative status.
         */
        Long qualityCheckJobs =
                entityManager.createQuery("""
                        SELECT COUNT(q)
                        FROM QualityCheck q
                        WHERE q.jobCard.garage.id IN :garageIds
                        AND q.status = :status
                        """, Long.class)
                        .setParameter(
                                "garageIds",
                                garageIds
                        )
                        .setParameter(
                                "status",
                                QualityCheckStatus.PENDING
                        )
                        .getSingleResult();

        Long readyForInvoiceJobs =
                countByStatus(
                        garageIds,
                        JobCardStatus.READY_FOR_INVOICE
                );

        /*
         * Current implementation uses INVOICE_GENERATED as the
         * real "invoice generated but payment not received" state.
         */
        Long paymentPendingJobs =
                countByStatus(
                        garageIds,
                        JobCardStatus.INVOICE_GENERATED
                );

        /*
         * ================================================================
         * LIFETIME GARAGE-SCOPED TOTALS
         * ================================================================
         */

        Long totalCustomers =
                entityManager.createQuery("""
                        SELECT COUNT(DISTINCT j.customer.id)
                        FROM JobCard j
                        WHERE j.garage.id IN :garageIds
                        """, Long.class)
                        .setParameter("garageIds", garageIds)
                        .getSingleResult();

        Long totalVehicles =
                entityManager.createQuery("""
                        SELECT COUNT(DISTINCT j.vehicle.id)
                        FROM JobCard j
                        WHERE j.garage.id IN :garageIds
                        """, Long.class)
                        .setParameter("garageIds", garageIds)
                        .getSingleResult();

        Long totalJobCards =
                entityManager.createQuery("""
                        SELECT COUNT(j)
                        FROM JobCard j
                        WHERE j.garage.id IN :garageIds
                        """, Long.class)
                        .setParameter("garageIds", garageIds)
                        .getSingleResult();

        Long totalInvoices =
                entityManager.createQuery("""
                        SELECT COUNT(i)
                        FROM Invoice i
                        WHERE i.estimate.jobCard.garage.id IN :garageIds
                        """, Long.class)
                        .setParameter("garageIds", garageIds)
                        .getSingleResult();

        /*
         * ================================================================
         * RECENT JOBS FOR SELECTED PERIOD
         * ================================================================
         */

        List<OwnerDashboardRecentJobResponse> recentJobs =
                getRecentJobs(
                        garageIds,
                        rangeStart,
                        rangeEnd
                );

        /*
         * ================================================================
         * RESPONSE
         * ================================================================
         */

        return OwnerDashboardSummaryResponse.builder()
                .from(from)
                .to(to)

                .jobsCreated(jobsCreated)
                .completedJobs(completedJobs)
                .revenue(revenue)

                .activeJobs(activeJobs)
                .pendingEstimates(pendingEstimates)
                .readyForDelivery(readyForDelivery)

                .inspectionJobs(inspectionJobs)
                .estimateJobs(estimateJobs)
                .repairJobs(repairJobs)
                .qualityCheckJobs(qualityCheckJobs)
                .readyForInvoiceJobs(readyForInvoiceJobs)
                .paymentPendingJobs(paymentPendingJobs)

                .totalCustomers(totalCustomers)
                .totalVehicles(totalVehicles)
                .totalJobCards(totalJobCards)
                .totalInvoices(totalInvoices)

                .recentJobs(recentJobs)

                .build();
    }

    private Long countByStatus(
            List<Long> garageIds,
            JobCardStatus status
    ) {

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

    private Long countByStatusIn(
            List<Long> garageIds,
            JobCardStatus... statuses
    ) {

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

    private List<OwnerDashboardRecentJobResponse> getRecentJobs(
            List<Long> garageIds,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {

        List<JobCard> jobs =
                entityManager.createQuery("""
                        SELECT j
                        FROM JobCard j
                        WHERE j.garage.id IN :garageIds
                        AND j.createdAt >= :rangeStart
                        AND j.createdAt < :rangeEnd
                        ORDER BY j.createdAt DESC
                        """, JobCard.class)
                        .setParameter("garageIds", garageIds)
                        .setParameter("rangeStart", rangeStart)
                        .setParameter("rangeEnd", rangeEnd)
                        .setMaxResults(10)
                        .getResultList();

        return jobs.stream()
                .map(job ->
                        OwnerDashboardRecentJobResponse.builder()
                                .jobCardId(job.getId())
                                .jobCardNumber(job.getJobCardNumber())
                                .customerName(
                                        job.getCustomer().getFullName()
                                )
                                .mobileNumber(
                                        job.getCustomer().getMobileNumber()
                                )
                                .registrationNumber(
                                        job.getVehicle()
                                                .getRegistrationNumber()
                                )
                                .vehicleName(
                                        job.getVehicle().getBrand()
                                                + " "
                                                + job.getVehicle().getModel()
                                )
                                .status(job.getStatus())
                                .serviceDate(job.getServiceDate())
                                .odometerReading(
                                        job.getOdometerReading()
                                )
                                .estimatedDeliveryDate(
                                        job.getEstimatedDeliveryDate()
                                )
                                .build()
                )
                .toList();
    }
}