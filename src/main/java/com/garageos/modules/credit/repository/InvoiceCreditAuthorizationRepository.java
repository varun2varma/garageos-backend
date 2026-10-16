package com.garageos.modules.credit.repository;

import com.garageos.core.enums.CreditAuthorizationStatus;
import com.garageos.modules.credit.entity.InvoiceCreditAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InvoiceCreditAuthorizationRepository extends JpaRepository<InvoiceCreditAuthorization, Long> {

    Optional<InvoiceCreditAuthorization> findFirstByInvoiceIdAndStatus(Long invoiceId, CreditAuthorizationStatus status);

    boolean existsByInvoiceIdAndStatus(Long invoiceId, CreditAuthorizationStatus status);

    List<InvoiceCreditAuthorization> findByInvoiceIdOrderByIdDesc(Long invoiceId);

    List<InvoiceCreditAuthorization> findByGarageIdInAndStatusOrderByDueDateAsc(
            Collection<Long> garageIds, CreditAuthorizationStatus status);
}
