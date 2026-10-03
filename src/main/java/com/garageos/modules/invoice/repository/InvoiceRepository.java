package com.garageos.modules.invoice.repository;

import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

}