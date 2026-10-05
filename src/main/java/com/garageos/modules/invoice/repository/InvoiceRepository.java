package com.garageos.modules.invoice.repository;

import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findTopByOrderByIdDesc();

    Optional<Invoice> findByEstimateId(Long estimateId);

    boolean existsByEstimateId(Long estimateId);

    Optional<Invoice> findByEstimateJobCardId(Long jobCardId);

    @Query("""
            SELECT COALESCE(SUM(i.grandTotal),0)
            FROM Invoice i
            WHERE i.generatedAt >= :start
            """)
    BigDecimal getTodayRevenue(LocalDateTime start);

    List<Invoice> findByEstimateJobCardCustomer(Customer customer);

    long countByEstimateJobCardCustomer(Customer customer);

    /** Garage-scoped / customer-scoped listings (no cross-tenant reads). */
    org.springframework.data.domain.Page<Invoice> findByEstimateJobCardGarageId(
            Long garageId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Invoice> findByEstimateJobCardCustomerId(
            Long customerId, org.springframework.data.domain.Pageable pageable);

    Optional<Invoice> findTopByEstimateJobCardGarageIdOrderByIdDesc(
            Long garageId);


    /** Row-locks the invoice so concurrent payment submissions serialise. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") Long id);

    org.springframework.data.domain.Page<Invoice> findByEstimateJobCardGarageIdIn(
            java.util.Collection<Long> garageIds, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Invoice> findByEstimateJobCardGarageIdInAndPaymentStatus(
            java.util.Collection<Long> garageIds,
            com.garageos.core.enums.PaymentStatus paymentStatus,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Invoice>
    findByEstimateJobCardGarageIdInAndInvoiceStatusInAndPaymentStatusIn(
            java.util.Collection<Long> garageIds,
            java.util.Collection<com.garageos.core.enums.InvoiceStatus> invoiceStatuses,
            java.util.Collection<com.garageos.core.enums.PaymentStatus> paymentStatuses,
            org.springframework.data.domain.Pageable pageable);

    /** [invoiceCount, grandTotal] of invoices still awaiting (full) payment. */
    @Query("""
            SELECT COUNT(i), COALESCE(SUM(i.grandTotal), 0) FROM Invoice i
            WHERE i.estimate.jobCard.garage.id IN :garageIds
              AND i.invoiceStatus IN (
                  com.garageos.core.enums.InvoiceStatus.GENERATED,
                  com.garageos.core.enums.InvoiceStatus.ACCEPTED)
              AND i.paymentStatus IN (
                  com.garageos.core.enums.PaymentStatus.PENDING,
                  com.garageos.core.enums.PaymentStatus.PARTIALLY_PAID)
            """)
    List<Object[]> outstandingTotals(@Param("garageIds") java.util.Collection<Long> garageIds);

    /** [count, subtotal, discount, gst, grandTotal] of issued invoices generated in the period. */
    @Query("""
            SELECT COUNT(i), COALESCE(SUM(i.subtotal), 0), COALESCE(SUM(i.discount), 0),
                   COALESCE(SUM(i.gst), 0), COALESCE(SUM(i.grandTotal), 0)
            FROM Invoice i
            WHERE i.estimate.jobCard.garage.id IN :garageIds
              AND i.invoiceStatus IN (
                  com.garageos.core.enums.InvoiceStatus.GENERATED,
                  com.garageos.core.enums.InvoiceStatus.ACCEPTED,
                  com.garageos.core.enums.InvoiceStatus.PAID)
              AND i.generatedAt >= :from AND i.generatedAt < :to
            """)
    List<Object[]> issuedTotals(
            @Param("garageIds") java.util.Collection<Long> garageIds,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
