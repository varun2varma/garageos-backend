package com.garageos.modules.payment.repository;

import com.garageos.core.enums.payment.PaymentMethod;
import com.garageos.modules.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByInvoiceIdOrderByPaidAtDescIdDesc(Long invoiceId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoice.id = :invoiceId")
    BigDecimal sumAmountByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query("""
            SELECT p.invoice.id, SUM(p.amount) FROM Payment p
            WHERE p.invoice.id IN :invoiceIds GROUP BY p.invoice.id
            """)
    List<Object[]> sumAmountByInvoiceIds(@Param("invoiceIds") Collection<Long> invoiceIds);

    @Query("""
            SELECT p FROM Payment p
            WHERE p.garageId IN :garageIds AND p.paidAt >= :from AND p.paidAt < :to
            """)
    Page<Payment> search(
            @Param("garageIds") Collection<Long> garageIds,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("""
            SELECT p FROM Payment p
            WHERE p.garageId IN :garageIds AND p.paidAt >= :from AND p.paidAt < :to
              AND p.method = :method
            """)
    Page<Payment> searchByMethod(
            @Param("garageIds") Collection<Long> garageIds,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("method") PaymentMethod method,
            Pageable pageable);

    /** [method, count, total] rows. */
    @Query("""
            SELECT p.method, COUNT(p), SUM(p.amount) FROM Payment p
            WHERE p.garageId IN :garageIds AND p.paidAt >= :from AND p.paidAt < :to
            GROUP BY p.method
            """)
    List<Object[]> summarizeByMethod(
            @Param("garageIds") Collection<Long> garageIds,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Already-collected part of invoices that are still PARTIALLY_PAID. */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
            WHERE p.garageId IN :garageIds
              AND p.invoice.paymentStatus = com.garageos.core.enums.PaymentStatus.PARTIALLY_PAID
              AND p.invoice.invoiceStatus IN (
                  com.garageos.core.enums.InvoiceStatus.GENERATED,
                  com.garageos.core.enums.InvoiceStatus.ACCEPTED)
            """)
    BigDecimal sumPaidOnPartiallyPaidInvoices(@Param("garageIds") Collection<Long> garageIds);
}
