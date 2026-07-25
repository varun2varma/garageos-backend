package com.garageos.modules.dashboard.repository;

import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.modules.dashboard.dto.response.DashboardSummaryResponse;
import com.garageos.modules.dashboard.dto.response.RecentJobResponse;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.vehicle.entity.Vehicle;
import com.garageos.modules.invoice.entity.Invoice;
import com.garageos.modules.estimate.entity.Estimate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardReadRepositoryImpl implements DashboardReadRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public DashboardSummaryResponse getDashboardSummary() {

        Long activeJobs = entityManager.createQuery("""
                SELECT COUNT(j)
                FROM JobCard j
                WHERE j.status NOT IN (:closed, :cancelled)
                """, Long.class)
                .setParameter("closed", JobCardStatus.CLOSED)
                .setParameter("cancelled", JobCardStatus.CANCELLED)
                .getSingleResult();

        Long pendingEstimates = entityManager.createQuery("""
                SELECT COUNT(e)
                FROM Estimate e
                WHERE e.status = :status
                """, Long.class)
                .setParameter("status", EstimateStatus.WAITING_FOR_APPROVAL)
                .getSingleResult();

        Long readyForDelivery = entityManager.createQuery("""
                SELECT COUNT(j)
                FROM JobCard j
                WHERE j.status = :status
                """, Long.class)
                .setParameter("status", JobCardStatus.READY_FOR_DELIVERY)
                .getSingleResult();

        Long completedToday = entityManager.createQuery("""
                SELECT COUNT(j)
                FROM JobCard j
                WHERE j.serviceDate = :today
                """, Long.class)
                .setParameter("today", LocalDate.now())
                .getSingleResult();

        BigDecimal todayRevenue = entityManager.createQuery("""
                SELECT COALESCE(SUM(i.grandTotal),0)
                FROM Invoice i
                WHERE i.generatedAt >= :today
                """, BigDecimal.class)
                .setParameter("today", LocalDate.now().atStartOfDay())
                .getSingleResult();

        Long inspectionJobs = entityManager.createQuery("""
                SELECT COUNT(j)
                FROM JobCard j
                WHERE j.status = :status
                """, Long.class)
                .setParameter("status", JobCardStatus.INSPECTION_PENDING)
                .getSingleResult();

        Long estimateJobs = entityManager.createQuery("""
                SELECT COUNT(j)
                FROM JobCard j
                WHERE j.status = :status
                """, Long.class)
                .setParameter("status", JobCardStatus.ESTIMATE_PENDING)
                .getSingleResult();

        Long repairJobs = entityManager.createQuery("""
                SELECT COUNT(j)
                FROM JobCard j
                WHERE j.status = :status
                """, Long.class)
                .setParameter("status", JobCardStatus.REPAIR_IN_PROGRESS)
                .getSingleResult();

        Long qualityCheckJobs = entityManager.createQuery("""
                SELECT COUNT(j)
                FROM JobCard j
                WHERE j.status = :status
                """, Long.class)
                .setParameter("status", JobCardStatus.QUALITY_CHECK)
                .getSingleResult();

        Long invoiceJobs = entityManager.createQuery("""
                SELECT COUNT(j)
                FROM JobCard j
                WHERE j.status = :status
                """, Long.class)
                .setParameter("status", JobCardStatus.READY_FOR_INVOICE)
                .getSingleResult();

        Long paymentPending = entityManager.createQuery("""
                SELECT COUNT(j)
                FROM JobCard j
                WHERE j.status = :status
                """, Long.class)
                .setParameter("status", JobCardStatus.PAYMENT_PENDING)
                .getSingleResult();

        Long totalCustomers = entityManager.createQuery("""
                SELECT COUNT(c)
                FROM Customer c
                """, Long.class)
                .getSingleResult();

        Long totalVehicles = entityManager.createQuery("""
                SELECT COUNT(v)
                FROM Vehicle v
                """, Long.class)
                .getSingleResult();

        Long totalJobCards = entityManager.createQuery("""
                SELECT COUNT(j)
                FROM JobCard j
                """, Long.class)
                .getSingleResult();

        Long totalInvoices = entityManager.createQuery("""
                SELECT COUNT(i)
                FROM Invoice i
                """, Long.class)
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

    @Override
    public List<RecentJobResponse> getRecentJobs() {

        List<JobCard> jobs = entityManager.createQuery("""
                SELECT j
                FROM JobCard j
                ORDER BY j.createdAt DESC
                """, JobCard.class)
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