package com.garageos.modules.invoice.repository;

import com.garageos.modules.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository
        extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findTopByOrderByIdDesc();
    Optional<Invoice> findByEstimateId(Long estimateId);
    boolean existsByEstimateId(Long estimateId);
    Optional<Invoice> findByEstimateJobCardId(Long jobCardId);

}